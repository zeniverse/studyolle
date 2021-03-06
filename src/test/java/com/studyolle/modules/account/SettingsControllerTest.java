package com.studyolle.modules.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyolle.infra.AbstractContainerBaseTest;
import com.studyolle.infra.MockMvcTest;
import com.studyolle.modules.account.AccountRepository;
import com.studyolle.modules.account.AccountService;
import com.studyolle.modules.account.form.SignUpForm;
import com.studyolle.modules.account.Account;
import com.studyolle.modules.tag.Tag;
import com.studyolle.modules.zone.Zone;
import com.studyolle.modules.tag.TagForm;
import com.studyolle.modules.zone.ZoneForm;
import com.studyolle.modules.tag.TagRepository;
import com.studyolle.modules.zone.ZoneRepository;
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

@MockMvcTest
class SettingsControllerTest extends AbstractContainerBaseTest {

    @Autowired MockMvc mockMvc;
    @Autowired AccountRepository accountRepository;
    @Autowired AccountService accountService;
    @Autowired ObjectMapper objectMapper;
    @Autowired TagRepository tagRepository;
    @Autowired ZoneRepository zoneRepository;

    private Zone testZone = Zone.builder().city("test")
                            .localNameOfCity("????????????")
                            .province("????????????").build();

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
    @DisplayName("?????? ?????? ?????? ???")
    @Test
    public void updateZoneForm() throws Exception {
        mockMvc.perform(get("/settings/zones"))
                .andExpect(view().name("settings/zones"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("zones"))
                .andExpect(model().attributeExists("whitelist"));

    }

    @WithUserDetails(value = "zering", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("?????? ?????? ??????")
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
    @DisplayName("?????? ?????? ??????")
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
    @DisplayName("?????? ?????? ???")
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
    @DisplayName("?????? ??????")
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
    @DisplayName("?????? ??????")
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
    @DisplayName("????????? ?????? ???")
    @Test
    public void updatedProfile_from() throws Exception {
        mockMvc.perform(get("/settings/profile"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("profile"));
    }

    @WithUserDetails(value = "zering", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("????????? ???????????? - ????????? ??????")
    @Test
    public void updatedProfile() throws Exception {
        String bio = "????????? bio??? ???????????? ??????";
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
    @DisplayName("????????? ???????????? - ????????? ??????")
    @Test
    public void updatedProfile_fail() throws Exception {
        String bio = "????????? bio??? ???????????? ???????????? ?????? ????????? bio??? ???????????? ???????????? ?????????????????????????????? ????????? bio??? ???????????? ???????????? ??????";
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
    @DisplayName("???????????? ?????? ???")
    @Test
    public void passwordForm() throws Exception {
        mockMvc.perform(get("/settings/password"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("passwordForm"));
    }

    @WithUserDetails(value = "zering", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("???????????? ?????? - ????????? ??????")
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
    @DisplayName("???????????? ?????? - ????????? ??????")
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