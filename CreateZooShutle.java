package org.matsim.prepare;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.TransitScheduleUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.VehicleType;

import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

public class CreateZooShutle {

    private static LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();
    private static NetworkFactory networkFactory = NetworkUtils.createNetwork().getFactory();
    private static TransitScheduleFactory scheduleFactory = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getTransitSchedule().getFactory();

    public static void main(String[] args) {

        var root = Paths.get("D:\\IdeaProjects\\matsim-serengeti-park-hodenhagen-zooshuttle\\scenarios\\serengeti-park-v1.0\\input");

        var network = NetworkUtils.readNetwork(root.resolve("serengeti-park-network-v1.0.xml.gz").toString());
        var scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        var vehicleType = scenario.getVehicles().getFactory().createVehicleType(Id.create("shuttle", VehicleType.class));
        vehicleType.setLength(20);
        vehicleType.setPcuEquivalents(2);
        vehicleType.setMaximumVelocity(36);
        vehicleType.setNetworkMode(TransportMode.pt);
        vehicleType.setDescription("shuttle vehicle type");
        vehicleType.getCapacity().setSeats(10000);
        vehicleType.getCapacity().setStandingRoom(0);
        scenario.getTransitVehicles().addVehicleType(vehicleType);

        var startNode = network.getFactory().createNode(Id.createNodeId("pt_start"), new Coord(544006.781992937 + 100, 5847658.641000098 + 100));
        network.addNode(startNode);
        var fromNode = network.getNodes().get(Id.createNodeId("29589035"));
        var toNode = network.getNodes().get(Id.createNodeId("3667621813"));
        var endNode = network.getFactory().createNode(Id.createNodeId("pt_end"), new Coord(541785.9309178652 - 100, 5844877.673792916 - 100));
        network.addNode(endNode);
        var startLink = createLink("pt_1", startNode, fromNode);
        var connection = createLink("pt2", fromNode, toNode);
        var endLink = createLink("pt_3", toNode, endNode);
        network.addLink(connection);
        network.addLink(startLink);
        network.addLink(endLink);

        var networkRoute = RouteUtils.createLinkNetworkRouteImpl(startLink.getId(), List.of(connection.getId()), endLink.getId());

        var fromStopFacility = scheduleFactory.createTransitStopFacility(Id.create("Stop_1", TransitStopFacility.class), startNode.getCoord(), false);
        fromStopFacility.setLinkId(startLink.getId());
        var toStopFacility = scheduleFactory.createTransitStopFacility(Id.create("Stop_2", TransitStopFacility.class), endNode.getCoord(), false);
        toStopFacility.setLinkId(endLink.getId());
        scenario.getTransitSchedule().addStopFacility(fromStopFacility);
        scenario.getTransitSchedule().addStopFacility(toStopFacility);

        var fromStop = scheduleFactory.createTransitRouteStop(fromStopFacility, 0, 0);
        var toStop = scheduleFactory.createTransitRouteStop(toStopFacility, 3600, 3610);

        var route = scheduleFactory.createTransitRoute(Id.create("route-1", TransitRoute.class), networkRoute, List.of(fromStop, toStop), "pt");

        // create departures and vehicles for each departure
        for (int i = 9 * 3600; i < 13 * 3600; i += 300) {
            var departure = scheduleFactory.createDeparture(Id.create("departure_" + i, Departure.class), i);
            var vehicle = scenario.getTransitVehicles().getFactory().createVehicle(Id.createVehicleId("shuttle_vehicle_" + i), vehicleType);
            departure.setVehicleId(vehicle.getId());

            scenario.getTransitVehicles().addVehicle(vehicle);
            route.addDeparture(departure);
        }

        var line = scheduleFactory.createTransitLine(Id.create("Shuttle", TransitLine.class));
        line.addRoute(route);
        scenario.getTransitSchedule().addTransitLine(line);

        new NetworkWriter(network).write(root.resolve("network-with-pt.xml.gz").toString());
        new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(root.resolve("transit-Schedule.xml.gz").toString());
        new MatsimVehicleWriter(scenario.getTransitVehicles()).writeFile(root.resolve("transit-vehicles.xml.gz").toString());
    }

    private static Link createLink(String id, Node from, Node to) {

        var connection = networkFactory.createLink(Id.createLinkId(id), from, to);
        connection.setAllowedModes(Set.of(TransportMode.pt));
        connection.setFreespeed(100);
        connection.setCapacity(10000);
        return connection;
    }
}
