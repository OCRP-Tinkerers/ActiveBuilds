package me.numilani.activebuilds.objects;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ConfigurationContents {
    int CheckInterval;
    List<BuildingType> Buildings = new ArrayList<>();

}
