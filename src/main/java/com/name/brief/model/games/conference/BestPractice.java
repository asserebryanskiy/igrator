package com.name.brief.model.games.conference;

import com.name.brief.model.BaseEntity;
import com.name.brief.model.games.Conference;
import lombok.*;
import org.hibernate.validator.constraints.Length;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@AllArgsConstructor
@ToString(exclude = "conference")
public class BestPractice extends BaseEntity {

    @ManyToOne
    private Conference conference;
    private Long participantId;
    @Column(length = 2048)
    private String text;

    public BestPractice() {
        super();
    }
}