package com._6.ems.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "salary")
public class Salary {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_code", referencedColumnName = "code")
    Personnel owner;

    @Column(name = "month")
    int month;

    @Column(name = "year")
    int year;

    @Column(name = "bonus")
    @Builder.Default
    double bonus = 0;

    @Column(name = "penalty")
    @Builder.Default
    double penalty = 0;

    @Column(name = "real_pay")
    @Builder.Default
    double realPay = 0;

    @Column(name = "half_day_work")
    @Builder.Default
    int halfWork = 0;

    @Column(name = "full_day_work")
    @Builder.Default
    int fullWork = 0;

    @Column(name = "absence_day")
    @Builder.Default
    int absence = 0;
}
