package com.maxdemarzi.processing.pagerank;

import com.maxdemarzi.processing.NodeCounter;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import org.neo4j.graphdb.*;

/**
 * @author mh
 * @since 28.03.15
 */
public class PageRankMapStorage implements PageRank {
    private final GraphDatabaseService db;
    private final int nodes;
    private Long2DoubleMap dstMap;

    public PageRankMapStorage(GraphDatabaseService db) {
        this.db = db;
        this.nodes = new NodeCounter().getNodeCount(db);
    }

    @Override
    public void compute(String label, String type, int iterations) {
        Long2DoubleMap srcMap = new Long2DoubleOpenHashMap();
        Long2LongMap degreeMap = new Long2LongOpenHashMap();
        dstMap = new Long2DoubleOpenHashMap(nodes);

        RelationshipType relationshipType = RelationshipType.withName(type);

        try ( Transaction tx = db.beginTx()) {
            ResourceIterator<Node> nodes = db.findNodes(Label.label(label));
            while (nodes.hasNext()) {
                Node node = nodes.next();
                srcMap.put(node.getId(), 0);
                dstMap.put(node.getId(), 0);
                degreeMap.put(node.getId(), node.getDegree(relationshipType, Direction.OUTGOING));
            }

            for (int iteration = 0; iteration < iterations; iteration++) {
                nodes = db.findNodes(DynamicLabel.label(label));
                while (nodes.hasNext()) {
                    Node node = nodes.next();
                    srcMap.put(node.getId(), ALPHA * dstMap.get(node.getId()) / degreeMap.get(node.getId()));
                    dstMap.put(node.getId(), ONE_MINUS_ALPHA);
                }

                for( Relationship relationship : db.getAllRelationships()) {
                    if (relationship.isType(relationshipType)) {
                        long x = relationship.getStartNode().getId();
                        long y = relationship.getEndNode().getId();
                        dstMap.put(y, (dstMap.get(y) + srcMap.get(x)));
                    }
                }
            }
            tx.success();
        }
    }

    @Override
    public double getResult(long node) {
        return dstMap != null ? dstMap.getOrDefault(node, -1D) : -1;
    }

    @Override
    public long numberOfNodes() {
        return nodes;
    }
}