package com.asg.common.lib.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "GLOBAL_DOC_MASTER")
public class DocumentEntity {
    @Id
    @Column(name = "DOC_ID", length = 20, nullable = false)
    @AuditIgnore
    private String docId;

    @Column(name = "DOC_SHORT_NAME", length = 100)
    private String docShortName;

    @Column(name = "DOC_SHORT_NAME2", length = 20)
    private String docShortName2;

    @Column(name = "DOC_NAME", length = 100, unique = true)
    private String docName;

    @Column(name = "DOC_NAME2", length = 20)
    private String docName2;

    @Column(name = "MODULE_ID", length = 20)
    private String moduleId;

    @Column(name = "DOC_TYPE", length = 20)
    private String docType;

    @Column(name = "ISO_DOCUMENT", length = 3)
    private String isoDocument;

    @Column(name = "DOC_REVISION")
    private BigDecimal docRevision;

    @Column(name = "DOC_REVISION_DATE")
    private java.sql.Date docRevisionDate;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "DOC_ICON")
    @AuditIgnore
    private byte[] docIcon;

    @Column(name = "DOC_DETAILS", length = 500)
    private String docDetails;

    @Column(name = "TASKFLOW_URL", length = 200)
    @AuditIgnore
    private String taskflowUrl;

    @Column(name = "SEQNO")
    private Integer seqno;

    @Column(name = "CREATED_BY", length = 20)
    @AuditIgnore
    private String createdBy;

    @Column(name = "CREATED_DATE")
    @AuditIgnore
    private java.sql.Timestamp createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    @AuditIgnore
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    @AuditIgnore
    private java.sql.Timestamp lastModifiedDate;

    @Column(name = "DOC_POID")
    @AuditIgnore
    private BigDecimal docPoid;

    @Column(name = "USER_ROLES", length = 200)
    private String userRoles;

    @Column(name = "APPROVAL_REQUIRED", length = 1)
    private String approvalRequired;

    @Column(name = "APPROVAL_REM_HRS")
    private BigDecimal approvalRemHrs;

    @Column(name = "APPROVAL_AUTO_HRS")
    private BigDecimal approvalAutoHrs;

    @Column(name = "ALERT_ON_CREATE", length = 1)
    private String alertOnCreate;

    @Column(name = "ALERT_USER_ROLE_POID", length = 50)
    private String alertUserRolePoid;

    @Column(name = "ALERT_CONDITIONAL_PROC", length = 100)
    private String alertConditionalProc;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "APPROVAL_INFO_FIELDS", length = 200)
    private String approvalInfoFields;

    @Column(name = "APPROVAL_VIEW_RPT_FILE", length = 50)
    private String approvalViewRptFile;

    @Column(name = "DATA_ENTRY_PERIOD", length = 2)
    private String dataEntryPeriod;

    @Column(name = "DEFAULT_LIST_PERIOD", length = 20)
    private String defaultListPeriod;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "ATTACHMENT_CHECKLIST", length = 200)
    private String attachmentChecklist;

    @Column(name = "APPROVAL_CUSTOM_RULE", length = 1)
    private String approvalCustomRule;

    @Column(name = "DOC_INFO_FIELDS_SQL", length = 500)
    private String docInfoFieldsSql;

    @Column(name = "APPROVAL_ALERTS", length = 1)
    private String approvalAlerts;

    @Column(name = "DEFAULT_SAVE_MODE", length = 20)
    private String defaultSaveMode;

    @Column(name = "DOC_PREFIX", length = 10)
    private String docPrefix;

    @Column(name = "APPROVAL_SHOW_IN_MAIN", length = 1)
    private String approvalShowInMain;

    @Column(name = "GL_POSTING", length = 1)
    private String glPosting;

    @Column(name = "GL_POSTING_DATE_FIELD", length = 35)
    private String glPostingDateField;

    @Column(name = "MAIN_TABLE_NAME", length = 50)
    private String mainTableName;

    @Column(name = "EDITABLE_ON_SAME_DAY", length = 1)
    private String editableOnSameDay;

    @Column(name = "HIDE_IN_MAIN_MENU", length = 1)
    private String hideInMainMenu;

    @Column(name = "LIST_OF_RECORDS_SQL", length = 4000)
    private String listOfRecordsSql;

    @Column(name = "LIST_OF_DISPLAY_COLUMNS_AND_TYPES", length = 4000)
    private String listOfDisplayColumnsAndTypes;

    @Column(name = "DOC_KEY_FIELD", length = 100)
    private String docKeyField;

    @Column(name = "DOC_RETURN_FIELDS", length = 500)
    private String docReturnFields;

    @Column(name = "ISO_DOCUMENT_NO", length = 30)
    private String isoDocumentNo;

    @Column(name = "ISO_ISSUE_NO", length = 30)
    private String isoIssueNo;

    @Column(name = "DOC_VALIDATION_FIELDS", length = 500)
    private String docValidationFields;

    @Column(name = "DOC_KEY_FIELD_NAME", length = 100)
    private String docKeyFieldName;

    @Column(name = "AUTO_REFRESH_FIELDS", length = 500)
    private String autoRefreshFields;

    @Column(name = "INVENTORY_DOCUMENT", length = 1)
    private String inventoryDocument;

    @Column(name = "INVENTORY_POSTING", length = 1)
    private String inventoryPosting;

    //Newly added on request, in table and stored procedure
    @Column(name = "UPLOAD_EDI", length = 1)
    private String uploadEdi;

    @Column(name = "PRINT", length = 1)
    private String print;

    @Column(name = "PREVIEW", length = 1)
    private String preview;

    @Column(name = "ENABLE_SLA", length = 1)
    private String enableSla;

    @Column(name = "SEND_EMAIL_SETTINGS", length = 500)
    private String sendEmailSettings;

    @Column(name = "SEND_EMAIL_ATTACHMENTS", length = 500)
    private String sendEmailAttachments;

    @Column(name = "ENABLE_COPY", length = 1)
    private String enableCopy;

    @Column(name = "DURATION")
    private Long duration;
}

