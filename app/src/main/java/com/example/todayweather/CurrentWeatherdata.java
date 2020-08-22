package com.example.todayweather;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CurrentWeatherdata {

    // set the data types of values that we want to extract

    String placename;
    String desc;
    Integer temp;
    Integer clouds;


    public CurrentWeatherdata(String placename, String desc, Integer temp, Integer clouds) {
        this.placename = placename;
        this.desc = desc;
        this.temp = temp;
        this.clouds = clouds;
    }

    public static CurrentWeatherdata serialize(String json) throws JSONException{

        JSONObject root = new JSONObject(json);
        String name = root.getString("name");
        Integer temp = root.getJSONObject("main").getInt("temp");
        Integer clouds = root.getJSONObject("clouds").getInt("all");

        String desc="";

        JSONArray weatherarray = root.getJSONArray("weather");
        if (weatherarray.length() > 0) {

            desc = ((JSONObject) weatherarray.get(0)).getString("description");
        }

        return new CurrentWeatherdata(name, desc, temp, clouds);


    }

    public String getPlacename() {
        return placename;
    }

    public String getDesc() {
        return desc;
    }

    public Integer getTemp() {
        return temp;
    }

    public Integer getClouds() {
        return clouds;
    }
}
