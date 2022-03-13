package com.studyolle.settings;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class PasswordForm {

    @Length(min = 3, max = 50)
    private String newPassword;

    @Length(min = 3, max = 50)
    private String newPasswordConfirm;

}

