package dbclient.trainstestschema;

import sqlcomponentizer.dbserializer.DBColumn;
import sqlcomponentizer.dbserializer.DBSerializable;

import java.time.LocalDateTime;

@DBSerializable(tableName = "schedules")
public class Schedules {

    @DBColumn(name = "id", isPrimaryKey = true)
    private Integer id;

    @DBColumn(name = "line_id", isForeignKey = true)
    private Integer lineID;

    @DBColumn(name = "departure_station_id", isForeignKey = true, foreignKeyReferences = "station_id")
    private Integer departureStationID;

    @DBColumn(name = "arrival_station_id", isForeignKey = true, foreignKeyReferences = "station_id")
    private Integer arrivalStationID;

    @DBColumn(name = "departure_time")
    private LocalDateTime departureTime;

    @DBColumn(name = "arrival_time")
    private LocalDateTime arrivalTime;

    public Schedules() {

    }

    public Schedules(Integer id, Integer lineID, Integer departureStationID, Integer arrivalStationID, LocalDateTime departureTime, LocalDateTime arrivalTime) {
        this.id = id;
        this.lineID = lineID;
        this.departureStationID = departureStationID;
        this.arrivalStationID = arrivalStationID;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
    }

    public Integer getId() {
        return id;
    }

    public Integer getLineID() {
        return lineID;
    }

    public Integer getDepartureStationID() {
        return departureStationID;
    }

    public Integer getArrivalStationID() {
        return arrivalStationID;
    }

    public LocalDateTime getDepartureTime() {
        return departureTime;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

}
