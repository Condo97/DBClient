package dbclient.trainstestschema;

import sqlcomponentizer.dbserializer.DBColumn;
import sqlcomponentizer.dbserializer.DBSerializable;
import sqlcomponentizer.dbserializer.DBSubObject;

import java.util.List;

@DBSerializable(tableName = "trainlines")
public class Trainlines {

    @DBColumn(name = "id", isPrimaryKey = true)
    private Integer id;

    @DBColumn(name = "name")
    private String name;

    @DBColumn(name = "description")
    private String description;

    @DBSubObject
    private List<Schedules> schedules;



    public Trainlines() {

    }

    public Trainlines(Integer id, String name, String description, List<Schedules> schedules) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.schedules = schedules;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<Schedules> getSchedules() {
        return schedules;
    }

}
