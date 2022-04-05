package com.studyolle.event;

import com.studyolle.account.AccountFactory;
import com.studyolle.account.AccountRepository;
import com.studyolle.account.AccountService;
import com.studyolle.account.SignUpForm;
import com.studyolle.domain.Account;
import com.studyolle.domain.Event;
import com.studyolle.domain.EventType;
import com.studyolle.domain.Study;
import com.studyolle.study.StudyFactory;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@RequiredArgsConstructor
class EventControllerTest {

    @Autowired AccountService accountService;
    @Autowired EventService eventService;
    @Autowired EventRepository eventRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired AccountFactory accountFactory;
    @Autowired StudyFactory studyFactory;
    @Autowired MockMvc mockMvc;
    @Autowired EnrollmentRepository enrollmentRepository;

    @BeforeEach
    public void beforeEach(){
        SignUpForm signUpForm = new SignUpForm();
        signUpForm.setEmail("zering@test.com");
        signUpForm.setNickname("zering");
        signUpForm.setPassword("123456789");
        accountService.processNewAccount(signUpForm);
    }

    @AfterEach
    void afterEach() {
        accountRepository.deleteAll();
    }

    @WithUserDetails(value = "zering", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("선착순 모임에 참가 신청 - 자동 수락")
    @Test
    public void newEnrollment_to_FCFS_event_accepted() throws Exception {
        Account testAccount = accountFactory.createAccount("testAccount");
        Study study = studyFactory.createStudy("test-study", testAccount);
        Event event = createEvent("test-event", EventType.FCFS, 2, study, testAccount);

        mockMvc.perform(post("/study/" + study.getPath() + "/events/" + event.getId() + "/enroll")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getPath() + "/events/" + event.getId()));

        Account zering = accountRepository.findByNickname("zering");
        isAccepted(zering, event);
    }

    @WithUserDetails(value = "zering", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("선착순 모임에 참가 신청 - 대기중")
    @Test
    public void newEnrollment_to_FCFS_event_not_accepted() throws Exception {
        Account testAccount = accountFactory.createAccount("testAccount");
        Study study = studyFactory.createStudy("test-study", testAccount);
        Event event = createEvent("test-event", EventType.FCFS, 2, study, testAccount);

        Account may = accountFactory.createAccount("may");
        Account june = accountFactory.createAccount("june");
        eventService.newEnrollment(event, may);
        eventService.newEnrollment(event, june);

        mockMvc.perform(post("/study/" + study.getPath() + "/events/" + event.getId() + "/enroll")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getPath() + "/events/" + event.getId()));

        Account zering = accountRepository.findByNickname("zering");
        isNotAccepted(zering, event);
    }

    @WithUserDetails(value = "zering", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("선착순 모임 - 참가 신청 확정자가 참가 취소하는 경우, 바로 다음 대기자를 자동으로 신청 확정")
    @Test
    public void accepted_account_cancelEnrollment_to_FCFS_event_not_accepted() throws Exception {
        Account zering = accountRepository.findByNickname("zering");
        Account testAccount = accountFactory.createAccount("testAccount");
        Account may = accountFactory.createAccount("may");
        Study study = studyFactory.createStudy("test-path", testAccount);
        Event event = createEvent("test-event", EventType.FCFS, 2, study, testAccount);

        eventService.newEnrollment(event, may);
        eventService.newEnrollment(event, zering);
        eventService.newEnrollment(event, testAccount);

        isAccepted(may, event);
        isAccepted(zering, event);
        isNotAccepted(testAccount, event);

        mockMvc.perform(post("/study/" + study.getPath() + "/events/" + event.getId() + "/disenroll")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getPath() + "/events/" + event.getId()));

        isAccepted(may, event);
        isAccepted(testAccount, event);
        assertNull(enrollmentRepository.findByEventAndAccount(event, zering));
    }

    @WithUserDetails(value = "zering", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("선착순 모임 - 참가 신청 비확정자가 참가 취소하는 경우, 기존 확정자를 그대로 유지하고 새로운 확정자는 없다.")
    @Test
    public void not_accepted_account_cancelEnrollment_to_FCFS_event_not_accepted() throws Exception {
        Account zering = accountRepository.findByNickname("zering");
        Account testAccount = accountFactory.createAccount("testAccount");
        Account may = accountFactory.createAccount("may");
        Study study = studyFactory.createStudy("test-path", testAccount);
        Event event = createEvent("test-event", EventType.FCFS, 2, study, testAccount);

        eventService.newEnrollment(event, may);
        eventService.newEnrollment(event, testAccount);
        eventService.newEnrollment(event, zering);

        isAccepted(may, event);
        isAccepted(testAccount, event);
        isNotAccepted(zering, event);

        mockMvc.perform(post("/study/" + study.getPath() + "/events/" + event.getId() + "/disenroll")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getPath() + "/events/" + event.getId()));

        isAccepted(may, event);
        isAccepted(testAccount, event);
        assertNull(enrollmentRepository.findByEventAndAccount(event, zering));
    }

    @WithUserDetails(value = "zering", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("관리자 신청 확인 모임 - 모임 참여 신청시 대기중 상태")
    @Test
    public void newEnrollment_to_CONFIMATIVE_event_not_accepted() throws Exception {
        Account testAccount = accountFactory.createAccount("testAccount");
        Study study = studyFactory.createStudy("test-study", testAccount);
        Event event = createEvent("test-event", EventType.CONFIRMATIVE, 2, study, testAccount);

        mockMvc.perform(post("/study/" + study.getPath() + "/events/" + event.getId() + "/enroll")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getPath() + "/events/" + event.getId()));

        Account zering = accountRepository.findByNickname("zering");
        isNotAccepted(zering, event);
    }

    private void isAccepted(Account zering, Event event) {
        assertTrue(enrollmentRepository.findByEventAndAccount(event, zering).isAccepted());
    }

    private void isNotAccepted(Account zering, Event event) {
        assertFalse(enrollmentRepository.findByEventAndAccount(event, zering).isAccepted());
    }


    private Event createEvent(String eventTitle, EventType eventType, int limit, Study study, Account account){
        Event event = new Event();
        event.setEventType(eventType);
        event.setLimitOfEnrollments(limit);
        event.setTitle(eventTitle);
        event.setCreatedDateTime(LocalDateTime.now());
        event.setEndEnrollmentDateTime(LocalDateTime.now().plusDays(1));
        event.setStartDateTime(LocalDateTime.now().plusDays(1).plusHours(5));
        event.setEndDateTime(LocalDateTime.now().plusDays(1).plusHours(7));
        return eventService.createEvent(event, study, account);
    }

}