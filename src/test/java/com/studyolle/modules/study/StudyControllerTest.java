package com.studyolle.modules.study;

import com.studyolle.modules.account.AccountRepository;
import com.studyolle.modules.account.AccountService;
import com.studyolle.modules.account.form.SignUpForm;
import com.studyolle.modules.account.Account;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@RequiredArgsConstructor
class StudyControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired StudyRepository studyRepository;
    @Autowired StudyService studyService;
    @Autowired AccountRepository accountRepository;
    @Autowired AccountService accountService;

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
    @DisplayName("스터디 개설 폼 조회")
    @Test
    public void createStudyForm() throws Exception {
        mockMvc.perform(get("/new-study"))
                .andExpect(status().isOk())
                .andExpect(view().name("study/form"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("studyForm"));
    }

    @WithUserDetails(value = "zering", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("스터디 개설 - 성공")
    @Test
    public void createStudy_success() throws Exception {
        mockMvc.perform(post("/new-study")
                .param("path", "test-path")
                .param("title", "Test Study Title")
                .param("shortDescription", "short description of a study")
                .param("fullDescription", "full description of a study")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/test-path"));

        Study byPath = studyRepository.findByPath("test-path");
        assertNotNull(byPath);
        Account zering = accountRepository.findByNickname("zering");
        assertTrue(byPath.getManagers().contains(zering));
    }

    @WithUserDetails(value = "zering", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("스터디 개설 - 실패")
    @Test
    public void createStudy_fail() throws Exception {
        mockMvc.perform(post("/new-study")
                .param("path", "wrong path")
                .param("title", "Test Study Title")
                .param("shortDescription", "short description of a study")
                .param("fullDescription", "full description of a study")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("study/form"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeExists("studyForm"))
                .andExpect(model().attributeExists("account"));

        Study byPath = studyRepository.findByPath("wrong path");
        assertNull(byPath);
    }

    @WithUserDetails(value = "zering", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("스터디 조회")
    @Test
    public void viewStudy() throws Exception {
        Study study = new Study();
        study.setPath("test-path");
        study.setTitle("test Study");
        study.setShortDescription("short description");
        study.setFullDescription("<p>full description</p>");

        Account zering = accountRepository.findByNickname("zering");
        studyService.createNewStudy(study, zering);

        mockMvc.perform(get("/study/test-path"))
                .andExpect(view().name("study/view"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("study"));
    }

    @WithUserDetails(value = "zering", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("스터디 가입")
    @Test
    public void joinStudy() throws Exception {
        Account testAccount = createAccount("testAccount");
        Study study = createStudy("test-study", testAccount);

        mockMvc.perform(get("/study/" + study.getPath() + "/join"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getPath() + "/members"));

        Account zering = accountRepository.findByNickname("zering");
        assertTrue(study.getMembers().contains(zering));

    }

    @WithUserDetails(value = "zering", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("스터디 탈퇴")
    @Test
    public void leaveStudy() throws Exception {
        Account testAccount = createAccount("testAccount");
        Study study = createStudy("test-study", testAccount);

        Account zering = accountRepository.findByNickname("zering");
        studyService.addMember(study, zering);

        mockMvc.perform(get("/study/" + study.getPath() + "/leave"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getPath() + "/members"));

        assertFalse(study.getMembers().contains(zering));

    }


    protected Study createStudy(String path, Account manager){
        Study study = new Study();
        study.setPath(path);
        studyService.createNewStudy(study, manager);
        return study;
    }

    protected Account createAccount(String nickname){
        Account account = new Account();
        account.setNickname(nickname);
        account.setEmail(nickname + "@email.com");
        accountRepository.save(account);
        return account;
    }
}