package bftsmart.optilog.monitors;

import bftsmart.reconfiguration.ServerViewController;
import org.jgrapht.Graph;
import org.jgrapht.alg.matching.GreedyMaximumCardinalityMatching;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class SuspicionGraph {

    private Graph<Integer, DefaultWeightedEdge> suspicionGraph = new DirectedWeightedMultigraph<>(DefaultWeightedEdge.class);
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private ServerViewController controller;

    public SuspicionGraph(ServerViewController controller) {
        this.controller = controller;
    }

    public synchronized void addSuspicion(int reporter, int suspect) {

        if (controller.getStaticConf().getProcessId() == 1) {
            logger.info("OptiLog > SuspicionGraph > addSuspicion called reporter {} , suspect {} ", reporter, suspect);
        }
        if (reporter == suspect) {
            return; // graph does not allow self-loops
        }

        suspicionGraph.addVertex(suspect);
        suspicionGraph.addVertex(reporter);

        DefaultWeightedEdge edge = suspicionGraph.getEdge(reporter, suspect);
        if (edge == null) {
            DefaultWeightedEdge suspicion = suspicionGraph.addEdge(reporter, suspect);
            suspicionGraph.setEdgeWeight(suspicion, 1.0); // add a suspicion
        } else {
            suspicionGraph.setEdgeWeight(edge, suspicionGraph.getEdgeWeight(edge) + 1.0); // Increase suspicion weight by 1
        }
    }

    public synchronized void removeSuspicions(double strength) {
        Set<DefaultWeightedEdge> edges = suspicionGraph.edgeSet();
        Set<DefaultWeightedEdge> toRemove = new LinkedHashSet<>();
        for (DefaultWeightedEdge edge : edges) {
            suspicionGraph.setEdgeWeight(edge, suspicionGraph.getEdgeWeight(edge) - strength); // Decrease suspicion strength
            if (suspicionGraph.getEdgeWeight(edge) <= 0) {
                toRemove.add(edge); // Remove suspicion
            }
        }
        if (!toRemove.isEmpty()) {
            suspicionGraph.removeAllEdges(toRemove);
        }
    }

    public synchronized void clearSuspicions() {
        suspicionGraph = new DirectedWeightedMultigraph<>(DefaultWeightedEdge.class);
    }

    public synchronized Set<Integer> candidateSet() {
        // Step 1: Compute maximal set of disjoint edges
        Graph<Integer, DefaultWeightedEdge> undirectedGraph = convertToUndirected(suspicionGraph);
        GreedyMaximumCardinalityMatching<Integer, DefaultWeightedEdge> matchingAlgo = new GreedyMaximumCardinalityMatching<>(undirectedGraph, false);
        Set<DefaultWeightedEdge> matchingEdges = matchingAlgo.getMatching().getEdges();

        logger.info(">>> OptiLog: SuspicionGraph: Maximal set of disjoint edges: {}", matchingEdges);

        // Step 2: Find all triangles in the graph
        Set<Set<Integer>> triangles = findTriangles(suspicionGraph);

        // Step 3: Identify vertices fulfilling the criteria from the paper:
        Set<Integer> condition = findVerticesMeetingConditions(suspicionGraph, triangles, matchingEdges);

        // Output the final result
        logger.info(">>> OptiLog: SuspicionGraph: Vertices fulfilling the second conditions: " + condition);

        // Init result set with *all* system nodes
        Set<Integer> resultSet = Arrays.stream(controller.getCurrentView().getProcesses()).boxed().collect(Collectors.toSet());

        // Now build the set that is excluded from being candidates:
        Set<Integer> exclusionList = new LinkedHashSet<>();
        for (DefaultWeightedEdge edge : matchingEdges) {
            exclusionList.add(suspicionGraph.getEdgeSource(edge));
            exclusionList.add(suspicionGraph.getEdgeTarget(edge));
        }
        exclusionList.addAll(condition); // Adds only non-duplicate elements

        // Ensure candidate set is at least of size f+1 so exclusion least cant be larger than n-f-1
        if (exclusionList.size() > (controller.getCurrentViewN() - controller.getCurrentViewF() - 1)) {
            exclusionList = reduceExclusionList(exclusionList);
        }
        resultSet.removeAll(exclusionList);

        logger.info(">>> OptiLog: SuspicionGraph: CandidateSet is " + resultSet);

        return resultSet;
    }

    // Find all triangles in the graph
    private Set<Set<Integer>> findTriangles(Graph<Integer, DefaultWeightedEdge> graph) {
        Set<Set<Integer>> triangles = new LinkedHashSet<>();

        for (Integer node : graph.vertexSet()) {
            for (Integer neighbor1 : graph.vertexSet()) {
                if (!graph.containsEdge(node, neighbor1) && !graph.containsEdge(neighbor1, node)) continue;
                for (Integer neighbor2 : graph.vertexSet()) {
                    if (node.equals(neighbor1) || node.equals(neighbor2) || neighbor1.equals(neighbor2)) continue;
                    if (graph.containsEdge(neighbor1, neighbor2) && graph.containsEdge(neighbor2, node)) {
                        Set<Integer> triangle = new TreeSet<>(Arrays.asList(node, neighbor1, neighbor2));
                        triangles.add(triangle);
                    }
                }
            }
        }
        return triangles;
    }

    // Find vertices that are:
    // 1. In a triangle
    // 2. Where the triangle has an edge in the matching
    // 3. Not adjacent to any edge in the matching
    private Set<Integer> findVerticesMeetingConditions(Graph<Integer, DefaultWeightedEdge> graph,
                                                       Set<Set<Integer>> triangles,
                                                       Set<DefaultWeightedEdge> matchingEdges) {
        Set<Integer> matchedVertices = new TreeSet<>();
        for (DefaultWeightedEdge edge : matchingEdges) {
            matchedVertices.add(graph.getEdgeSource(edge));
            matchedVertices.add(graph.getEdgeTarget(edge));
        }

        Set<Integer> resultSet = new TreeSet<>();
        for (Set<Integer> triangle : triangles) {
            boolean hasMatchingEdge = false;
            for (DefaultWeightedEdge edge : matchingEdges) {
                Integer u = graph.getEdgeSource(edge);
                Integer v = graph.getEdgeTarget(edge);
                if (triangle.contains(u) && triangle.contains(v)) {
                    hasMatchingEdge = true;
                    break;
                }
            }
            if (hasMatchingEdge) {
                for (Integer vertex : triangle) {
                    if (!matchedVertices.contains(vertex)) {
                        resultSet.add(vertex);
                    }
                }
            }
        }
        return resultSet;
    }

    // Make sure exclusionList will not grow beyond the size of (n-f-1) so f+1 candidates are always available
    private Set<Integer> reduceExclusionList(Set<Integer> suspects) {

        LinkedList<Integer> sortedList = new LinkedList<>(suspects);
        // Sort by degree (in-degree), ascending order
        sortedList.sort((a, b) -> Integer.compare(suspicionGraph.inDegreeOf(a), suspicionGraph.inDegreeOf(b)));
        while (sortedList.size() > controller.getCurrentViewN() - controller.getCurrentViewF() - 1) {
            sortedList.removeFirst();
        }
        suspects = new TreeSet<>(sortedList);
        return suspects;
    }

    public void printGraphAscii() {
        System.out.println("Suspicion Graph (ASCII Representation):");

        // Store formatted edges
        List<String> edgeRepresentations = new ArrayList<>();

        for (DefaultWeightedEdge edge : suspicionGraph.edgeSet()) {
            int source = suspicionGraph.getEdgeSource(edge);
            int target = suspicionGraph.getEdgeTarget(edge);
            double weight = suspicionGraph.getEdgeWeight(edge);

            // Format the edge visually
            edgeRepresentations.add(String.format("%d --(%.2f)--> %d", source, weight, target));
        }

        // Print all edges
        if (edgeRepresentations.isEmpty()) {
            System.out.println("[Graph is empty]");
        } else {
            edgeRepresentations.forEach(System.out::println);
        }
    }

    private Graph<Integer, DefaultWeightedEdge> convertToUndirected(Graph<Integer, DefaultWeightedEdge> directedGraph) {
        // Create an undirected graph
        Graph<Integer, DefaultWeightedEdge> undirectedGraph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

        // Add all vertices
        for (Integer vertex : directedGraph.vertexSet()) {
            undirectedGraph.addVertex(vertex);
        }

        // Find bidirectional edges
        Set<DefaultWeightedEdge> toKeep = new LinkedHashSet<>();

        for (DefaultWeightedEdge edge : directedGraph.edgeSet()) {
            Integer source = directedGraph.getEdgeSource(edge);
            Integer target = directedGraph.getEdgeTarget(edge);

            // Check if the reverse edge exists
            if (directedGraph.containsEdge(target, source)) {
                toKeep.add(edge);
            }
        }

        // Add only bidirectional edges to the new undirected graph
        for (DefaultWeightedEdge edge : toKeep) {
            Integer source = directedGraph.getEdgeSource(edge);
            Integer target = directedGraph.getEdgeTarget(edge);

            if (source != null & target != null) {
                undirectedGraph.addEdge(source, target);
            }
        }

        return undirectedGraph;
    }

}
