package com.studyolle.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyolle.account.AccountRepository;
import com.studyolle.account.AccountService;
import com.studyolle.account.SignUpForm;
import com.studyolle.account.Account;
import com.studyolle.tag.Tag;
import com.studyolle.zone.Zone;
import com.studyolle.tag.TagForm;
import com.studyolle.zone.ZoneForm;
import com.studyolle.tag.TagRepository;
import com.studyolle.zone.ZoneRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
class SettingsControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired AccountRepository accountRepository;
    @Autowired AccountService accountService;
    @Autowired ObjectMapper objectMapper;
    @Autowired TagRepository tagRepository;
    @Autowired ZoneRepository zoneRepository;

    private Zone testZone = Zone.builder().city("test")
                            .localNameOfCity("테스트시")
                            .province("테스트주").build();

    @BeforeEach
    public void beforeEach(){
        SignUpForm signUpForm = new SignUpForm();
        signUpForm.setEmail("zering@test.com");
        signUpForm.setNickname("zering");
        signUpForm.setPassword("123456789");
        accountService.processNewAccount(signUpForm);

        zoneRepository.save(testZone);
    }

    @AfterEach
    void afterEach() {
        accountRepository.deleteAll();
        zoneRepository.deleteAll();
    }

    @WithUserDetails(value = "zering", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("활동 지역 수정 폼")
    @Test
    public void updateZoneForm() throws Exception {
        mockMvc.perform(get("/settings/zones"))
                .andExpect(view().name("settings/zones"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("zones"))
                .andExpect(model().attributeExists("whitelist"));

    }

    @WithUserDetails(value = "zering", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("지역 정보 추가")
    @Test
    public void addZone() throws Exception {

        ZoneForm zoneForm = new ZoneForm();
        zoneForm.setZoneName(testZone.toString());

        mockMvc.perform(post("/settings/zones/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(zoneForm))
                .with(csrf()))
                .andExpect(status().isOk());

        Account zering = accountRepository.findByNickname("zering");
        Zone zone = zoneRepository.findByCityAndProvince(zoneForm.getCityName(), zoneForm.getProvince());
        assertTrue(zering.getZones().contains(zone));
    }

    @WithUserDetails(value = "zering", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("지역 정보 제거")
    @Test
    public void removeZone() throws Exception {

        Account zering = accountRepository.findByNickname("zering");
        Zone zone = zoneRepository.findByCityAndProvince(testZone.getCity(), testZone.getProvince());
        accountService.addZone(zering, zone);

        ZoneForm zoneForm = new ZoneForm();
        zoneForm.setZoneName(testZone.toString());

        mockMvc.perform(post("/settings/zones/remove")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(zoneForm))
                .with(csrf()))
                .andExpect(status().isOk());

        assertFalse(zering.getZones().contains(zone));

    }

    @WithUserDetails(value = "zering", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("태그 수정 폼")
    @Test
    public void updatedTagsfrom() throws Exception {
        mockMvc.perform(get("/settings/tags"))
                .andExpect(status().isOk())
                .andExpect(view().name("settings/tags"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("whitelist"))
                .andExpect(model().attributeExists("tags"));
    }

    @WithUserDetails(value = "zering", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("태그 추가")
    @Test
    public void addTag() throws Exception {
        TagForm tagForm = new TagForm();
        tagForm.setTagTitle("newTag");

        mockMvc.perform(post("/settings/tags/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tagForm))
                .with(csrf()))
                .andExpect(status().isOk());

        Tag newTag = tagRepository.findByTitle("newTag");
        assertNotNull(newTag);
        Account zering = accountRepository.findByNickname("zering");
        assertTrue(zering.getTags().contains(newTag));

    }

    @WithUserDetails(value = "zering", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("태그 삭제")
    @Test
    public void removeTag() throws Exception {

        Account zering = accountRepository.findByNickname("zering");
        Tag newTag = tagRepository.save(Tag.builder().title("newTag").build());
        accountService.addTag(zering, newTag);

        assertTrue(zering.getTags().contains(newTag));

        TagForm tagForm = new TagForm();
        tagForm.setTagTitle("newTag");

        mockMvc.perform(post("/settings/tags/remove")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tagForm))
                .with(csrf()))
                .andExpect(status().isOk());

        assertFalse(zering.getTags().contains("newTag"));

    }


    @WithUserDetails(value = "zering", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("프로필 수정 폼")
    @Test
    public void updatedProfile_from() throws Exception {
        mockMvc.perform(get("/settings/profile"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("profile"));
    }

    @WithUserDetails(value = "zering", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("프로필 수정하기 - 입력값 정상")
    @Test
    public void updatedProfile() throws Exception {
        String bio = "프로필 bio를 수정하는 경우";
        mockMvc.perform(post("/settings/profile")
                .param("bio", bio)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/settings/profile"))
                .andExpect(flash().attributeExists("message"));

        Account zering = accountRepository.findByNickname("zering");
        assertEquals(bio,zering.getBio());

    }

    @WithUserDetails(value = "zering", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("프로필 수정하기 - 입력값 에러")
    @Test
    public void updatedProfile_fail() throws Exception {
        String bio = "프로필 bio를 길게길게 수정하는 경우 프로필 bio를 길게길게 수정하는 수정하는수정하는경우 프로필 bio를 길게길게 수정하는 경우";
        mockMvc.perform(post("/settings/profile")
                .param("bio", bio)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("settings/profile"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("profile"))
                .andExpect(model().hasErrors());

        Account zering = accountRepository.findByNickname("zering");
        assertNull(zering.getBio());

    }

    @WithUserDetails(value = "zering", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("패스워드 수정 폼")
    @Test
    public void passwordForm() throws Exception {
        mockMvc.perform(get("/settings/password"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("passwordForm"));
    }

    @WithUserDetails(value = "zering", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("패스워드 수정 - 입력값 정상")
    @Test
    public void updatePassword() throws Exception {
        mockMvc.perform(post("/settings/password")
                .param("newPassword", "aaaaaaaa")
                .param("newPasswordConfirm", "aaaaaaaa")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/settings/password"))
                .andExpect(flash().attributeExists("message"));
    }

    @WithUserDetails(value = "zering", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("패스워드 수정 - 입력값 에러")
    @Test
    public void updatePassword_fail() throws Exception {
        mockMvc.perform(post("/settings/password")
                .param("newPassword", "11")
                .param("newPasswordConfirm", "11")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("settings/password"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("passwordForm"));
    }

}