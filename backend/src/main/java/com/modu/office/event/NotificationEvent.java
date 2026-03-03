package com.modu.office.event;

import com.modu.office.dto.NotificationPayload;
import com.modu.office.entity.AppUser;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NotificationEvent extends ApplicationEvent {
    private final AppUser targetUser;
    private final NotificationPayload payload;

    public NotificationEvent(Object source, AppUser targetUser, NotificationPayload payload) {
        super(source);
        this.targetUser = targetUser;
        this.payload = payload;
    }
}
