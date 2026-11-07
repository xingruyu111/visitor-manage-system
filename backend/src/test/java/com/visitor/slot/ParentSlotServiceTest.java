package com.visitor.slot;

import com.visitor.common.exception.BizException;
import com.visitor.domain.entity.ParentSlot;
import com.visitor.server.mapper.ParentSlotMapper;
import com.visitor.server.service.ParentSlotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ParentSlotServiceTest {

    @Autowired
    private ParentSlotService service;

    @Autowired
    private ParentSlotMapper mapper;

    private final LocalDate monday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

    @BeforeEach
    void setUp() {
        mapper.delete(null);
    }

    @Test
    void crudWorks() {
        ParentSlot created = service.create(slot(1, LocalTime.of(8, 0), LocalTime.of(10, 0)));
        assertThat(service.list("13900001111")).hasSize(1);

        ParentSlot update = slot(2, LocalTime.of(9, 0), LocalTime.of(11, 0));
        assertThat(service.update(created.getId(), update).getWeekday()).isEqualTo(2);

        service.delete(created.getId());
        assertThat(mapper.selectCount(null)).isZero();
    }

    @Test
    void validationRejectsWrongWeekdayAndMissingSlot() {
        service.create(slot(1, LocalTime.of(8, 0), LocalTime.of(10, 0)));

        LocalDateTime ok = monday.atTime(9, 0);
        assertThatCode(() -> service.validate("13900001111", ok, ok.plusHours(1)))
                .doesNotThrowAnyException();

        LocalDateTime tuesday = monday.plusDays(1).atTime(9, 0);
        assertThatThrownBy(() -> service.validate("13900001111", tuesday, tuesday.plusHours(1)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不在家长固定时段");

        assertThatThrownBy(() -> service.validate("13900002222", ok, ok.plusHours(1)))
                .hasMessageContaining("未配置");
    }

    @Test
    void outsideTimeRangeIsRejected() {
        service.create(slot(1, LocalTime.of(8, 0), LocalTime.of(10, 0)));

        LocalDateTime late = monday.atTime(10, 30);
        assertThatThrownBy(() -> service.validate("13900001111", late, late.plusMinutes(30)))
                .hasMessageContaining("不在家长固定时段");
    }

    @Test
    void invalidBodyIsRejected() {
        ParentSlot bad = slot(1, LocalTime.of(10, 0), LocalTime.of(9, 0));
        assertThatThrownBy(() -> service.create(bad)).hasMessageContaining("结束时间");

        ParentSlot badWeekday = slot(9, LocalTime.of(8, 0), LocalTime.of(9, 0));
        assertThatThrownBy(() -> service.create(badWeekday)).hasMessageContaining("星期");

        assertThatThrownBy(() -> service.update(999L, slot(1, LocalTime.of(8, 0), LocalTime.of(9, 0))))
                .hasMessageContaining("时段不存在");
        assertThatThrownBy(() -> service.delete(999L)).hasMessageContaining("时段不存在");
    }

    private ParentSlot slot(int weekday, LocalTime start, LocalTime end) {
        ParentSlot slot = new ParentSlot();
        slot.setVisitorPhone("13900001111");
        slot.setWeekday(weekday);
        slot.setStartTime(start);
        slot.setEndTime(end);
        slot.setEffectiveFrom(monday.minusDays(7));
        slot.setEffectiveTo(monday.plusDays(7));
        return slot;
    }
}
