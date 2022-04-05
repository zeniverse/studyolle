package com.studyolle.modules.zone;

import lombok.Data;

@Data
public class ZoneForm {

    private String zoneName;

    public String getCityName(){
        return zoneName.substring(0, zoneName.indexOf("("));
    }

    public String getLocalNameOfCity(){
        return zoneName.substring(zoneName.indexOf("(") + 1, zoneName.indexOf(")"));
    }

    public String getProvince(){
        return zoneName.substring(zoneName.indexOf("/") + 1);
    }

    public Zone getZone(){
        return Zone.builder().city(this.getCityName())
                .localNameOfCity(this.getLocalNameOfCity())
                .province(this.getProvince()).build();
    }

}
