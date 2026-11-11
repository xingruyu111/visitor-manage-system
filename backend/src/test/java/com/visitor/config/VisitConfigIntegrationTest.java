package com.visitor.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.visitor.common.enums.RoleEnum;
import com.visitor.common.exception.BizException;
import com.visitor.domain.dto.BookingRule;
import com.visitor.domain.dto.VisitorTypeSaveDTO;
import com.visitor.domain.entity.User;
import com.visitor.domain.entity.UserRole;
import com.visitor.domain.entity.VisitorPass;
import com.visitor.domain.entity.VisitorRecord;
import com.visitor.domain.entity.VisitorType;
import com.visitor.server.mapper.UserMapper;
import com.visitor.server.mapper.UserRoleMapper;
import com.visitor.server.mapper.VisitorPassMapper;
import com.visitor.server.mapper.VisitorRecordMapper;
import com.visitor.server.mapper.VisitorTypeMapper;
import com.visitor.server.service.AlumniMonthlyPassService;
import com.visitor.server.service.VisitConfigService;
import com.visitor.server.service.VisitorTypeService;
import com.visitor.server.controller.admin.VisitConfigController;
import com.visitor.server.controller.visitor.VisitorTypeController;
import com.visitor.server.mapper.VisitConfigMapper;
import com.visitor.domain.entity.VisitConfig;
import com.visitor.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class VisitConfigIntegrationTest {

    @Autowired
    private VisitorTypeService visitorTypeService;

    @Autowired
    private VisitConfigService visitConfigService;

    @Autowired
    private AlumniMonthlyPassService alumniMonthlyPassService;

    @Autowired
    private VisitorTypeMapper visitorTypeMapper;

    @Autowired
    private VisitorRecordMapper recordMapper;

    @Autowired
    private VisitorPassMapper passMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private VisitConfigController visitConfigController;

    @Autowired
    private VisitorTypeController visitorTypeController;

    @Autowired
    private VisitConfigMapper visitConfigMapper;

    private Long alumniUserId;

    @BeforeEach
    void setUp() {
        passMapper.delete(null);
        recordMapper.delete(null);
        userRoleMapper.delete(null);
        userMapper.delete(null);
        visitorTypeMapper.delete(new LambdaQueryWrapper<VisitorType>()
                .likeRight(VisitorType::getCode, "TEST_"));
        visitConfigService.updateAlumniMonths(List.of());

        User user = new User();
        user.setPhone("13800006001");
        user.setName("校友测试");
        user.setPassword("x");
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        alumniUserId = user.getId();

        UserRole role = new UserRole();
        role.setUserId(alumniUserId);
        role.setRole(RoleEnum.ALUMNI);
        role.setSchoolId(1L);
        role.setIsDefault(1);
        role.setSource("MANUAL");
        role.setStatus(1);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        userRoleMapper.insert(role);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void bookingRulesRestrictWeekdayAndTime() {
        VisitorType type = createType("TEST_WEEKLY", 2, "WEEKLY", List.of("1"));
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        visitorTypeService.validateBookingTime(type.getCode(), monday.atTime(9, 0), monday.atTime(10, 0));

        LocalDate tuesday = monday.plusDays(1);
        assertThatThrownBy(() -> visitorTypeService.validateBookingTime(
                type.getCode(), tuesday.atTime(9, 0), tuesday.atTime(10, 0)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("可预约时间段");
    }

    @Test
    void visitorTypeCrudAndPassEntries() {
        VisitorType type = createType("TEST_CRUD", 5, "UNLIMITED", List.of());
        assertThat(visitorTypeService.passMaxEntries(type.getCode())).isEqualTo(5);

        VisitorTypeSaveDTO update = new VisitorTypeSaveDTO();
        update.setCode("TEST_CRUD");
        update.setName("改名类型");
        update.setMaxEntries(0);
        update.setStatus(1);
        update.setSortNo(9);
        BookingRule rule = new BookingRule();
        rule.setMode("UNLIMITED");
        rule.setValues(List.of());
        update.setBookingRules(List.of(rule));
        VisitorType updated = visitorTypeService.update(type.getId(), update);

        assertThat(updated.getName()).isEqualTo("改名类型");
        assertThat(visitorTypeService.passMaxEntries(type.getCode())).isEqualTo(999999);
        assertThat(visitorTypeService.updateStatus(type.getId(), 0).getStatus()).isZero();
        assertThat(visitorTypeService.list(true))
                .noneMatch(item -> item.getCode().equals("TEST_CRUD"));
    }

    @Test
    void alumniMonthsCreateNaturalMonthPassOnce() {
        int month = LocalDate.now().getMonthValue();
        visitConfigService.updateAlumniMonths(List.of(month));
        assertThat(visitConfigService.alumniMonths()).containsExactly(month);

        UserContext.set(alumniUserId, "校友测试", RoleEnum.ALUMNI, 1L);
        Map<String, Object> first = alumniMonthlyPassService.currentMonth();
        assertThat(first.get("eligible")).isEqualTo(true);
        Long recordId = ((Number) first.get("recordId")).longValue();

        VisitorRecord record = recordMapper.selectById(recordId);
        assertThat(record.getTargetType()).isEqualTo("ALUMNI_MONTH");
        assertThat(record.getVisitorType()).isEqualTo("ALUMNI");
        assertThat(record.getExpectedTime().toLocalDate()).isEqualTo(LocalDate.now().withDayOfMonth(1));
        VisitorPass pass = passMapper.selectOne(new LambdaQueryWrapper<VisitorPass>()
                .eq(VisitorPass::getRecordId, recordId));
        assertThat(pass.getMaxEntries()).isEqualTo(999999);

        Map<String, Object> second = alumniMonthlyPassService.currentMonth();
        assertThat(((Number) second.get("recordId")).longValue()).isEqualTo(recordId);
        assertThat(recordMapper.selectCount(new LambdaQueryWrapper<VisitorRecord>()
                .eq(VisitorRecord::getTargetType, "ALUMNI_MONTH"))).isEqualTo(1);
    }

    @Test
    void otherBookingModesMatchDates() {
        LocalDate today = LocalDate.now();
        LocalDate nextMonth = today.plusMonths(1).withDayOfMonth(1);

        VisitorType daily = createType("TEST_DAILY", 1, "DAILY", List.of("15"));
        final LocalDate dailyDate = today.getDayOfMonth() <= 15 ? today.withDayOfMonth(15) : today.plusMonths(1).withDayOfMonth(15);
        visitorTypeService.validateBookingTime(daily.getCode(), dailyDate.atTime(9, 0), dailyDate.atTime(10, 0));

        VisitorType monthly = createType("TEST_MONTHLY", 1, "MONTHLY", List.of(String.valueOf(nextMonth.getMonthValue())));
        visitorTypeService.validateBookingTime(monthly.getCode(),
                nextMonth.atTime(9, 0), nextMonth.atTime(10, 0));

        VisitorType monthDay = createType("TEST_MONTH_DAY", 1, "MONTH_DAY",
                List.of(nextMonth.getMonthValue() + "-" + nextMonth.getDayOfMonth()));
        visitorTypeService.validateBookingTime(monthDay.getCode(),
                nextMonth.atTime(9, 0), nextMonth.atTime(10, 0));

        assertThatThrownBy(() -> visitorTypeService.validateBookingTime(monthDay.getCode(),
                nextMonth.plusDays(1).atTime(9, 0), nextMonth.plusDays(1).atTime(10, 0)))
                .isInstanceOf(BizException.class);
    }

    @Test
    void invalidRulesAndDuplicateCodesAreRejected() {
        assertThatThrownBy(() -> createType("TEST_BAD_MODE", 1, "BAD", List.of("1")))
                .isInstanceOf(BizException.class);
        assertThatThrownBy(() -> createType("TEST_EMPTY_WEEK", 1, "WEEKLY", List.of()))
                .isInstanceOf(BizException.class);

        VisitorType type = createType("TEST_DUP", 1, "UNLIMITED", List.of());
        createType("TEST_ANOTHER", 1, "UNLIMITED", List.of());
        assertThatThrownBy(() -> createType("TEST_DUP", 1, "UNLIMITED", List.of()))
                .isInstanceOf(BizException.class);

        VisitorTypeSaveDTO update = new VisitorTypeSaveDTO();
        update.setCode("TEST_ANOTHER");
        update.setName("冲突");
        update.setMaxEntries(1);
        update.setStatus(1);
        assertThatThrownBy(() -> visitorTypeService.update(type.getId(), update))
                .isInstanceOf(BizException.class);
        assertThatThrownBy(() -> visitorTypeService.updateStatus(type.getId(), 9))
                .isInstanceOf(BizException.class);
    }

    @Test
    void invalidAlumniMonthsAreRejectedAndNonAlumniIsIneligible() {
        assertThatThrownBy(() -> visitConfigService.updateAlumniMonths(List.of(0)))
                .isInstanceOf(BizException.class);
        assertThatThrownBy(() -> visitConfigService.updateAlumniMonths(List.of(13)))
                .isInstanceOf(BizException.class);

        UserContext.set(alumniUserId, "校友测试", RoleEnum.TEACHER, 1L);
        assertThat(alumniMonthlyPassService.currentMonth().get("eligible")).isEqualTo(false);
    }

    @Test
    void malformedBookingJsonFailsClosed() {
        VisitorType type = createType("TEST_BAD_JSON", 1, "UNLIMITED", List.of());
        type.setBookingRulesJson("{bad-json");
        visitorTypeMapper.updateById(type);

        assertThatThrownBy(() -> visitorTypeService.validateBookingTime(
                type.getCode(), LocalDateTime.now(), LocalDateTime.now().plusHours(1)))
                .isInstanceOf(BizException.class);
    }

    @Test
    void configControllersAndMissingConfigRowAreCovered() {
        visitConfigMapper.delete(null);
        assertThat(visitConfigService.alumniMonths()).isEmpty();
        assertThat(visitConfigController.alumniMonths().data().get("months")).isEqualTo(List.of());

        assertThat(visitConfigController.updateAlumniMonths(Map.of("months", List.of(1, 8))).data()
                .get("months")).isEqualTo(List.of(1, 8));
        assertThat(visitConfigController.types().code()).isEqualTo(200);
        assertThat(visitorTypeController.types().code()).isEqualTo(200);

        VisitorTypeSaveDTO dto = new VisitorTypeSaveDTO();
        dto.setCode("TEST_CONTROLLER");
        dto.setName("控制器类型");
        dto.setMaxEntries(1);
        dto.setStatus(1);
        dto.setBookingRules(List.of(rule("UNLIMITED", List.of())));
        var created = visitConfigController.create(dto).data();
        assertThat(created.getCode()).isEqualTo("TEST_CONTROLLER");
        assertThat(visitConfigController.status(created.getId(), 0).data().getStatus()).isZero();
    }

    @Test
    void malformedAlumniConfigFallsBackToEmpty() {
        VisitConfig config = visitConfigMapper.selectOne(new LambdaQueryWrapper<VisitConfig>()
                .eq(VisitConfig::getSchoolId, 1L));
        config.setAlumniMonthsJson("{bad");
        visitConfigMapper.updateById(config);
        assertThat(visitConfigService.alumniMonths()).isEmpty();
    }
    private BookingRule rule(String mode, List<String> values) {
        BookingRule rule = new BookingRule();
        rule.setMode(mode);
        rule.setValues(values);
        return rule;
    }

    private VisitorType createType(String code, int maxEntries, String mode, List<String> values) {
        VisitorTypeSaveDTO dto = new VisitorTypeSaveDTO();
        dto.setCode(code);
        dto.setName("测试类型");
        dto.setMaxEntries(maxEntries);
        dto.setStatus(1);
        dto.setSortNo(99);
        BookingRule rule = new BookingRule();
        rule.setMode(mode);
        rule.setValues(values);
        if ("WEEKLY".equals(mode)) {
            rule.setStartTime("08:00");
            rule.setEndTime("18:00");
        }
        dto.setBookingRules(List.of(rule));
        return visitorTypeService.create(dto);
    }
}
