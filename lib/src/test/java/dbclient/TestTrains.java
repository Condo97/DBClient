package dbclient;

import dbclient.trainstestschema.Schedules;
import dbclient.trainstestschema.Stations;
import dbclient.trainstestschema.Trainlines;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestTrains {

    @Test
    @DisplayName("Test deepinsert")
    void testDeepInsert() {
        Trainlines trainlines1 = new Trainlines(
                null,
                "line 1",
                "The first line ever!"
        );

        Trainlines trainlines2 = new Trainlines(
                null,
                "line 2",
                "The second line, better than the first"
        );

        Stations station1 = new Stations(
                null,
                "station 1",
                123.0,
                123.0
        );

        Stations station2 = new Stations(
                null,
                "station 2",
                321.0,
                321.0
        );

        Schedules schedule1 = new Schedules(
                null,

        )
    }

}
