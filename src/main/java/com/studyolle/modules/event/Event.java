package com.studyolle.modules.event;

import com.studyolle.modules.account.Account;
import com.studyolle.modules.account.UserAccount;
import com.studyolle.modules.study.Study;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@NamedEntityGraph(
        name = "Event.withEnrollments",
        attributeNodes = @NamedAttributeNode("enrollments")
)

@Entity
@Getter @Setter @EqualsAndHashCode(of = "id")
public class Event {

    @Id @GeneratedValue
    private Long id;

    @ManyToOne
    private Study study;

    @ManyToOne
    private Account createdBy;

    @Column(nullable = false)
    private String title;

    @Lob
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdDateTime;

    @Column(nullable = false)
    private LocalDateTime endEnrollmentDateTime;

    @Column(nullable = false)
    private LocalDateTime startDateTime;

    @Column(nullable = false)
    private LocalDateTime endDateTime;

    private Integer limitOfEnrollments;

    @OneToMany(mappedBy = "event")
    @OrderBy("enrolledAt")
    private List<Enrollment> enrollments = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private EventType eventType;

    public boolean isEnrollableFor(UserAccount userAccount){
        return isNotClosed() && !isAttended(userAccount) && !isAlreadyEnrolled(userAccount);
    }

    private boolean isNotClosed() {
        return this.endEnrollmentDateTime.isAfter(LocalDateTime.now());
    }

    private boolean isAlreadyEnrolled(UserAccount userAccount) {
        Account account = userAccount.getAccount();

        for(Enrollment e : this.enrollments){
            if(e.getAccount().equals(account)){
                return true;
            }
        }
        return false;
    }

    public boolean isDisenrollableFor(UserAccount userAccount){
        return isNotClosed() && !isAttended(userAccount) && isAlreadyEnrolled(userAccount);
    }

    public boolean isAttended(UserAccount userAccount){
        Account account = userAccount.getAccount();

        for(Enrollment e : this.enrollments){
            if(e.getAccount().equals(account) && e.isAttended()){
                return true;
            }
        }

        return false;
    }

    public int numberOfRemainSpots() {
        long count = 0L;
        for (Enrollment enrollment : this.enrollments) {
            if (enrollment.isAccepted()) {
                count++;
            }
        }
        return this.limitOfEnrollments - (int) count;
    }

    public long getNumberOfAcceptedEnrollments() {
        return this.enrollments.stream().filter(Enrollment::isAccepted).count();
    }

    public boolean canAccept(Enrollment enrollment){
        return this.eventType == EventType.CONFIRMATIVE
                && this.enrollments.contains(enrollment)
                && !enrollment.isAttended()
                && !enrollment.isAccepted();
    }

    public boolean canReject(Enrollment enrollment){
        return this.eventType == EventType.CONFIRMATIVE
                && this.enrollments.contains(enrollment)
                && !enrollment.isAttended()
                && enrollment.isAccepted();
    }

    public boolean isAbleToAcceptWaitingEnrollment() {
        return this.getEventType() == EventType.FCFS &&
                this.limitOfEnrollments > this.getNumberOfAcceptedEnrollments();

    }

    public void addEnrollment(Enrollment enrollment) {
        this.enrollments.add(enrollment);
        enrollment.setEvent(this);
    }

    public void removeEnrollment(Enrollment enrollment) {
        this.enrollments.remove(enrollment);
        enrollment.setEvent(null);
    }

    public void acceptNextWaitingEnrollment() {
        if(isAbleToAcceptWaitingEnrollment()){
            Enrollment enrollmentToAccept = this.getTheFirstWaitingEnrollment();
            if(enrollmentToAccept != null){
                enrollmentToAccept.setAccepted(true);
            }
        }
    }

    private Enrollment getTheFirstWaitingEnrollment() {
        for(Enrollment e : this.enrollments){
            if(!e.isAccepted()){
                return e;
            }
        }

        return null;
    }

    private List<Enrollment> getWaitingList() {
        List<Enrollment> list = new ArrayList<>();
        for (Enrollment enrollment : this.enrollments) {
            if (!enrollment.isAccepted()) {
                list.add(enrollment);
            }
        }
        return list;
    }

    public void acceptWaitingList() {
        if(isAbleToAcceptWaitingEnrollment()){
            var waitingList = getWaitingList();
            int numberToAccept =
                    (int) Math.min(
                            this.limitOfEnrollments - this.getNumberOfAcceptedEnrollments(),
                            waitingList.size()
                    );
            waitingList.subList(0, numberToAccept).forEach(e -> e.setAccepted(true));
        }
    }

    public void accept(Enrollment enrollment) {
        if(this.eventType == EventType.CONFIRMATIVE
        && this.limitOfEnrollments > getNumberOfAcceptedEnrollments()){
            enrollment.setAccepted(true);
        }
    }

    public void reject(Enrollment enrollment) {
        if(this.eventType == EventType.CONFIRMATIVE){
            enrollment.setAccepted(false);
        }
    }
}
