package dbclient.trainstestschema;

import sqlcomponentizer.dbserializer.DBColumn;
import sqlcomponentizer.dbserializer.DBSerializable;

@DBSerializable(tableName = "stations")
public class Stations {

    @DBColumn(name = "id", isPrimaryKey = true)
    private Integer id;

    @DBColumn(name = "name")
    private String name;

    @DBColumn(name = "latitude")
    private Double latitude;

    @DBColumn(name = "longitude")
    private Double longitude;

    public Stations() {

    }

    public Stations(Integer id, String name, Double latitude, Double longitude) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

}
