package com.studyolle.modules.event;

import com.studyolle.infra.AbstractContainerBaseTest;
import com.studyolle.infra.MockMvcTest;
import com.studyolle.modules.account.AccountFactory;
import com.studyolle.modules.account.AccountRepository;
import com.studyolle.modules.account.AccountService;
import com.studyolle.modules.account.form.SignUpForm;
import com.studyolle.modules.account.Account;
import com.studyolle.modules.study.Study;
import com.studyolle.modules.study.StudyFactory;
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


@MockMvcTest
class EventControllerTest extends AbstractContainerBaseTest {

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
    @DisplayName("????????? ????????? ?????? ?????? - ?????? ??????")
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
    @DisplayName("????????? ????????? ?????? ?????? - ?????????")
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
    @DisplayName("????????? ?????? - ?????? ?????? ???????????? ?????? ???????????? ??????, ?????? ?????? ???????????? ???????????? ?????? ??????")
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
    @DisplayName("????????? ?????? - ?????? ?????? ??????????????? ?????? ???????????? ??????, ?????? ???????????? ????????? ???????????? ????????? ???????????? ??????.")
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
    @DisplayName("????????? ?????? ?????? ?????? - ?????? ?????? ????????? ????????? ??????")
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