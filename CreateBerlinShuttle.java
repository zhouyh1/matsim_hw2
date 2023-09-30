package org.matsim.prepare;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.VehicleType;

import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

public class CreateBerlinShuttle {

    private static LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();
    private static NetworkFactory networkFactory = NetworkUtils.createNetwork().getFactory();
    private static TransitScheduleFactory scheduleFactory = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getTransitSchedule().getFactory();

    public static void main(String[] args) {

        var root = Paths.get("D:\\IdeaProjects\\matsim-berlin\\scenarios\\berlin-v5.5-1pct\\input");
        var network = NetworkUtils.readNetwork(root.resolve("berlin-v5.5-network.xml.gz").toString());
        var scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        var vehicleType = scenario.getVehicles().getFactory().createVehicleType(Id.create("shuttle", VehicleType.class));
        vehicleType.setLength(20);
        vehicleType.setPcuEquivalents(2);
        vehicleType.setMaximumVelocity(36);
        vehicleType.setNetworkMode(TransportMode.pt);
        vehicleType.setDescription("shuttle vehicle type");
        vehicleType.getCapacity().setSeats(50);
        vehicleType.getCapacity().setStandingRoom(50);
        scenario.getTransitVehicles().addVehicleType(vehicleType);

        var stop1Node = network.getNodes().get(Id.createNodeId("4414079830"));
        var stop2Node = network.getNodes().get(Id.createNodeId("26704063"));
        var stop3Node = network.getNodes().get(Id.createNodeId("26822715"));
        var stop4Node = network.getNodes().get(Id.createNodeId("1831965425"));

        var connection1 = createLink("pt1", stop1Node, stop2Node);
        var connection2 = createLink("pt2", stop2Node, stop3Node);
        var connection3 = createLink("pt3", stop3Node, stop4Node);

        network.addLink(connection1);
        network.addLink(connection2);
        network.addLink(connection3);

        var networkRoute = RouteUtils.createLinkNetworkRouteImpl(connection1.getId(),List.of(connection2.getId()),connection3.getId());

        var Stop1Facility = scheduleFactory.createTransitStopFacility(Id.create("Stop_1", TransitStopFacility.class), stop1Node.getCoord(), false);
        Stop1Facility.setLinkId(connection1.getId());
        var Stop2Facility = scheduleFactory.createTransitStopFacility(Id.create("Stop_2", TransitStopFacility.class), stop2Node.getCoord(), false);
        Stop2Facility.setLinkId(connection1.getId());
        var Stop3Facility = scheduleFactory.createTransitStopFacility(Id.create("Stop_3", TransitStopFacility.class), stop3Node.getCoord(), false);
        Stop3Facility.setLinkId(connection2.getId());
        var Stop4Facility = scheduleFactory.createTransitStopFacility(Id.create("Stop_4", TransitStopFacility.class), stop4Node.getCoord(), false);
        Stop4Facility.setLinkId(connection3.getId());

        scenario.getTransitSchedule().addStopFacility(Stop1Facility);
        scenario.getTransitSchedule().addStopFacility(Stop2Facility);
        scenario.getTransitSchedule().addStopFacility(Stop3Facility);
        scenario.getTransitSchedule().addStopFacility(Stop4Facility);

        var stop1 = scheduleFactory.createTransitRouteStop(Stop1Facility, 0, 0);
        var stop2 = scheduleFactory.createTransitRouteStop(Stop2Facility, 0.5 * 3600, 0.5 * 3600 + 30);
        var stop3 = scheduleFactory.createTransitRouteStop(Stop3Facility, 0.75 * 3600, 0.75 * 3600 + 30);
        var stop4 = scheduleFactory.createTransitRouteStop(Stop4Facility, 0.916 * 3600, 0.916 * 3600 + 30);

        var route = scheduleFactory.createTransitRoute(Id.create("route-1", TransitRoute.class), networkRoute, List.of(stop1, stop2, stop3, stop4), "pt");

        // create departures and vehicles for each departure
        for (int i = 7 * 3600; i < 19 * 3600; i += 1800) {
            var departure = scheduleFactory.createDeparture(Id.create("departure_" + i, Departure.class), i);
            var vehicle = scenario.getTransitVehicles().getFactory().createVehicle(Id.createVehicleId("shuttle_vehicle_" + i), vehicleType);
            departure.setVehicleId(vehicle.getId());

            scenario.getTransitVehicles().addVehicle(vehicle);
            route.addDeparture(departure);
        }

        var line = scheduleFactory.createTransitLine(Id.create("Shuttle", TransitLine.class));
        line.addRoute(route);
        scenario.getTransitSchedule().addTransitLine(line);

        new NetworkWriter(network).write(root.resolve("BerlinShuttle_network-with-pt.xml.gz").toString());
        new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(root.resolve("BerlinShuttle_transit-Schedule.xml.gz").toString());
        new MatsimVehicleWriter(scenario.getTransitVehicles()).writeFile(root.resolve("BerlinShuttle_transit-vehicles.xml.gz").toString());
    }

    private static Link createLink(String id, Node from, Node to) {

        var connection = networkFactory.createLink(Id.createLinkId(id), from, to);
        connection.setAllowedModes(Set.of(TransportMode.pt));
        connection.setFreespeed(100);
        connection.setCapacity(10000);
        return connection;
    }
}
