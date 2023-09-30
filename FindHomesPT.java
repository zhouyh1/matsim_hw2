package org.matsim.analysis;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;

public class FindHomesPT {

    public static void main(String[] args) {

         // Replace "some-name" with the name of your new highway link

        var population = PopulationUtils.readPopulation("D:\\IdeaProjects\\matsim-serengeti-park-hodenhagen-zooshuttle\\scenarios\\serengeti-park-v1.0\\output\\output-serengeti-park-v1.0-pt-M1\\serengeti-park-v1.0-run1.output_plans.xml.gz");
        var features = ShapeFileReader.getAllFeatures("D:\\IdeaProjects\\matsim-serengeti-park-hodenhagen-zooshuttle\\original-input-data\\shp-files\\serengeti-park\\serengeti-park.shp");
        var transformation = TransformationFactory.getCoordinateTransformation("EPSG:25832", "EPSG:3857");


        int counter = 0;
        double totalDistance = 0.0;
        //double totalTime = 0.0;


        for (Person person : population.getPersons().values()) {
            var plan = person.getSelectedPlan();
            var firstElement = plan.getPlanElements().get(0);
            var homeactivity = (Activity) firstElement;
            var homeactivityCoord = homeactivity.getCoord();
            var transformedActCoord = transformation.transform(homeactivityCoord);
            var homeactivityPoint = MGC.coord2Point(transformedActCoord);

            // Match person's location to shape file
            //String personLocation = matchLocationToShapeFile(homeactivityPoint, features);
            //if (personLocation == null) {
                //continue; // Skip person if location is not found in shape file
            //}
            String modeName = "pt";
            for (int i = 1; i < plan.getPlanElements().size(); i++) {
                var planElement = plan.getPlanElements().get(i);
                if (planElement instanceof Leg) {
                    var leg = (Leg) planElement;
                    var modeValue = leg.getMode();
                    if (modeValue.contains(modeName)) {
                        counter++;
                        System.out.println("Person ID: " + person.getId() + ", Home: " + homeactivityCoord + "," + counter);
                        break;

                    }
                }
            }
        }

    }

    //private static String matchLocationToShapeFile(Point homeactivityPoint, Collection<SimpleFeature> features) {
        //for (SimpleFeature feature : features) {
            //Geometry geometry = (Geometry) feature.getDefaultGeometry();
            //if (geometry.contains(homeactivityPoint)) {
                //return feature.getAttribute("Gemeinde_n").toString();
            //}
        //}
        //return null; // Location not found in shape file
    //}
}

//var traveldistance = leg.getRoute() .getDistance();
//totalDistance += traveldistance;

//var traveltime = leg.getRoute() .getTravelTime();
//totalTime += traveltime;
//System.out.println(personLocation);
//System.out.println("Person ID: " + person.getId() + ", Home: " + homeactivityPoint + ", Travel distance: " + totalDistance + "," + counter);