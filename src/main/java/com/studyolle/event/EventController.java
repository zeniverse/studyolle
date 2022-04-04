package com.studyolle.event;

import com.studyolle.account.CurrentAccount;
import com.studyolle.domain.Account;
import com.studyolle.domain.Enrollment;
import com.studyolle.domain.Event;
import com.studyolle.domain.Study;
import com.studyolle.event.form.EventForm;
import com.studyolle.event.validator.EventFormValidator;
import com.studyolle.study.StudyRepository;
import com.studyolle.study.StudyService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/study/{path}")
@RequiredArgsConstructor
public class EventController {

    private final StudyService studyService;
    private final EventFormValidator eventFormValidator;
    private final ModelMapper modelMapper;
    private final EventService eventService;
    private final EventRepository eventRepository;
    private final StudyRepository studyRepository;
    private final EnrollmentRepository enrollmentRepository;

    @InitBinder("eventForm")
    public void initBinder(WebDataBinder webDataBinder){
        webDataBinder.addValidators(eventFormValidator);
    }

    @GetMapping("/new-event")
    public String newEventForm(@CurrentAccount Account account, @PathVariable String path, Model model){
        Study study = studyService.getStudyToUpdateStatus(account, path);

        model.addAttribute(account);
        model.addAttribute(study);
        model.addAttribute(new EventForm());

        return "event/form";
    }

    @PostMapping("/new-event")
    public String newEventSubmit(@CurrentAccount Account account, @PathVariable String path,
                                 @Valid EventForm eventForm, Errors errors, Model model){
        Study study = studyService.getStudyToUpdateStatus(account, path);

        if(errors.hasErrors()){
            model.addAttribute(account);
            model.addAttribute(study);
            return "event/form";
        }

        Event event = eventService.createEvent(modelMapper.map(eventForm, Event.class), study, account);
        return "redirect:/study/" + study.getEncodedPath() + "/events/" + event.getId();
    }

    @GetMapping("/events/{id}")
    public String getEvent(@CurrentAccount Account account, @PathVariable String path,
                           @PathVariable Long id, Model model){
        model.addAttribute(account);
        model.addAttribute(studyRepository.findStudyWithManagersByPath(path));
        model.addAttribute(eventRepository.findById(id).orElseThrow());

        return "event/view";
    }

    @GetMapping("/events")
    public String viewStudyEvents(@CurrentAccount Account account, @PathVariable String path, Model model){
        Study study = studyService.getStudy(path);
        model.addAttribute(account);
        model.addAttribute(study);

        List<Event> events = eventRepository.findByStudyOrderByStartDateTime(study);
        List<Event> oldEvents = new ArrayList<>();
        List<Event> newEvents = new ArrayList<>();

        events.forEach(e -> {
            if(e.getEndDateTime().isBefore(LocalDateTime.now())){
                oldEvents.add(e);
            }else{
                newEvents.add(e);
            }
        });

        model.addAttribute("newEvents", newEvents);
        model.addAttribute("oldEvents", oldEvents);

        return "study/events";
    }

    @GetMapping("/events/{id}/edit")
    public String updateEventForm(@CurrentAccount Account account, @PathVariable String path,
                                  @PathVariable Long id, Model model){
        Study study = studyService.getStudyToUpdate(account, path);
        Event event = eventRepository.findById(id).orElseThrow();

        model.addAttribute(account);
        model.addAttribute(study);
        model.addAttribute(event);
        model.addAttribute(modelMapper.map(event, EventForm.class));

        return "event/update-form";
    }

    @PostMapping("/events/{id}/edit")
    public String updateEventSubmit(@CurrentAccount Account account, @PathVariable String path, @PathVariable Long id,
                                    @Valid EventForm eventForm, Errors errors, Model model){
        Study study = studyService.getStudyToUpdate(account, path);
        Event event = eventRepository.findById(id).orElseThrow();
        eventForm.setEventType(event.getEventType());

        eventFormValidator.validateUpdateForm(eventForm, event, errors);

        if(errors.hasErrors()){
            model.addAttribute(account);
            model.addAttribute(study);
            model.addAttribute(event);

            return "event/update-form";
        }

        eventService.updateEvent(event, eventForm);

        return "redirect:/study/" + study.getEncodedPath() + "/events/" + event.getId();
    }

//    @PostMapping("/events/{id}/delete")
//    public String cancelEvent(@CurrentAccount Account account, @PathVariable String path,
//                              @PathVariable Long id){
//        Study study = studyService.getStudyToUpdateStatus(account, path);
//        Event event = eventRepository.findById(id).orElseThrow();
//
//        eventService.deleteEvent(event);
//
//        return "redirect:/study/" + study.getEncodedPath() + "/events";
//    }

    @DeleteMapping("/events/{id}")
    public String cancelEvent(@CurrentAccount Account account, @PathVariable String path,
                              @PathVariable Long id){
        Study study = studyService.getStudyToUpdateStatus(account, path);
        Event event = eventRepository.findById(id).orElseThrow();

        eventService.deleteEvent(event);

        return "redirect:/study/" + study.getEncodedPath() + "/events";
    }

    @PostMapping("/events/{id}/enroll")
    public String newEnrollment(@CurrentAccount Account account, @PathVariable String path,
                                @PathVariable Long id){
        Study study = studyService.getStudyToEnroll(path);
        Event event = eventRepository.findById(id).orElseThrow();

        eventService.newEnrollment(event, account);
        return "redirect:/study/" + study.getEncodedPath() + "/events/" + id;
    }

    @PostMapping("/events/{id}/disenroll")
    public String cancelEnrollment(@CurrentAccount Account account, @PathVariable String path,
                                @PathVariable Long id){
        Study study = studyService.getStudyToEnroll(path);
        Event event = eventRepository.findById(id).orElseThrow();

        eventService.cancelEnrollment(event, account);

        return "redirect:/study/" + study.getEncodedPath() + "/events/" + id;
    }



    @GetMapping("/events/{eventId}/enrollments/{enrollmentId}/accept")
    public String acceptEnrollment(@CurrentAccount Account account, @PathVariable String path,
                                   @PathVariable Long eventId, @PathVariable Long enrollmentId){
        Study study = studyService.getStudyToUpdate(account, path);
        Event event = eventRepository.findById(eventId).orElseThrow();
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId).orElseThrow();

        eventService.acceptEnrollment(event, enrollment);

        return "redirect:/study/" + study.getEncodedPath() + "/events/" + eventId;
    }

    @GetMapping("/events/{eventId}/enrollments/{enrollmentId}/reject")
    public String rejectEnrollment(@CurrentAccount Account account, @PathVariable String path,
                                   @PathVariable Long eventId, @PathVariable Long enrollmentId){
        Study study = studyService.getStudyToUpdate(account, path);
        Event event = eventRepository.findById(eventId).orElseThrow();
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId).orElseThrow();

        eventService.rejectEnrollment(event, enrollment);

        return "redirect:/study/" + study.getEncodedPath() + "/events/" + eventId;
    }


    @GetMapping("/events/{eventId}/enrollments/{enrollmentId}/checkin")
    public String checkInEnrollment(@CurrentAccount Account account, @PathVariable String path,
                                   @PathVariable Long eventId, @PathVariable Long enrollmentId){
        Study study = studyService.getStudyToUpdate(account, path);
        Event event = eventRepository.findById(eventId).orElseThrow();
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId).orElseThrow();

        eventService.checkInEnrollment(enrollment);

        return "redirect:/study/" + study.getEncodedPath() + "/events/" + eventId;
    }

    @GetMapping("/events/{eventId}/enrollments/{enrollmentId}/cancel-checkin")
    public String cancelCheckinInEnrollment(@CurrentAccount Account account, @PathVariable String path,
                                    @PathVariable Long eventId, @PathVariable Long enrollmentId){
        Study study = studyService.getStudyToUpdate(account, path);
        Event event = eventRepository.findById(eventId).orElseThrow();
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId).orElseThrow();

        eventService.cancelCheckInEnrollment(enrollment);

        return "redirect:/study/" + study.getEncodedPath() + "/events/" + eventId;
    }
}
