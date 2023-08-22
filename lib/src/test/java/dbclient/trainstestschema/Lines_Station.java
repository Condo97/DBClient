package dbclient.trainstestschema;

import sqlcomponentizer.dbserializer.DBColumn;
import sqlcomponentizer.dbserializer.DBSerializable;

@DBSerializable(tableName = "lines_stations")
public class Lines_Station {

    @DBColumn(name = "id", isPrimaryKey = true)
    private Integer id;

    @DBColumn(name = "line_id", isForeignKey = true)
    private Integer lineID;

    @DBColumn(name = "station_id", isForeignKey = true)
    private Integer stationID;

    @DBColumn(name = "stop_sequence")
    private Integer stopSequence;

    public Lines_Station() {

    }

    public Lines_Station(Integer id, Integer lineID, Integer stationID, Integer stopSequence) {
        this.id = id;
        this.lineID = lineID;
        this.stationID = stationID;
        this.stopSequence = stopSequence;
    }

    public Integer getId() {
        return id;
    }

    public Integer getLineID() {
        return lineID;
    }

    public Integer getStationID() {
        return stationID;
    }

    public Integer getStopSequence() {
        return stopSequence;
    }

}
