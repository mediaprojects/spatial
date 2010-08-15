package org.neo4j.gis.spatial;

import org.neo4j.gis.spatial.LayerTests.TestRelationshipTypes;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.Traverser.Order;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * Simple encoder that stores geometries as an linked list of point
 * nodes. Only supports LineString geometries.
 * 
 * @TODO: Consider generalizing this code and making a general linked
 *        list geometry store available in the library
 * @author craig
 */
class SimpleGraphEncoder extends AbstractGeometryEncoder {
	private GeometryFactory geometryFactory;

	private GeometryFactory getGeometryFactory() {
		if(geometryFactory==null) geometryFactory = new GeometryFactory();
		return geometryFactory;
	}

	private Node testIsNode(PropertyContainer container) {
		if (!(container instanceof Node)) {
			throw new SpatialDatabaseException("Cannot decode non-node geometry: " + container);
		}
		return (Node) container;
	}

	@Override
	protected void encodeGeometryShape(Geometry geometry, PropertyContainer container) {
		Node node = testIsNode(container);
		node.setProperty("gtype", GTYPE_LINESTRING);
		Node prev = null;
		for (Coordinate coord : geometry.getCoordinates()) {
			Node point = node.getGraphDatabase().createNode();
			point.setProperty("x", coord.x);
			point.setProperty("y", coord.y);
			point.setProperty("z", coord.z);
			if (prev == null) {
				node.createRelationshipTo(point, TestRelationshipTypes.FIRST);
			} else {
				prev.createRelationshipTo(point, TestRelationshipTypes.NEXT);
			}
			prev = point;
		}
	}

	public Geometry decodeGeometry(PropertyContainer container) {
		Node node = testIsNode(container);
		CoordinateList coordinates = new CoordinateList();
		for (Node point : node.traverse(Order.DEPTH_FIRST, StopEvaluator.END_OF_GRAPH, ReturnableEvaluator.ALL_BUT_START_NODE,
		        TestRelationshipTypes.FIRST, Direction.OUTGOING, TestRelationshipTypes.NEXT, Direction.OUTGOING)) {
			coordinates.add(new Coordinate((Double) point.getProperty("x"), (Double) point.getProperty("y"), (Double) point.getProperty("z")), false);
		}
		return getGeometryFactory().createLineString(coordinates.toCoordinateArray());
	}
}