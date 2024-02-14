package com.tictactoegame.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="champions")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Champions {
    @Id
    @Column(name = "pid")
    private int pid;

    @Column(name = "name")
    private String name;

    @Column(name = "role")
    private String role;

    @Column(name = "difficulty")
    private String difficulty;

    @Column(name = "region")
    private String region;

    @Column(name = "release_date")
    private String releaseDate;

    @Column(name = "ability_resource")
    private String abilityResource;

    @Column(name = "melee_ranged")
    private String meleeRanged;

    @Column(name = "gender")
    private String gender;
}
