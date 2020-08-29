package io.metersphere.base.domain;

import java.io.Serializable;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;

@Data
public class User implements Serializable {

    private String id;

    @NotEmpty(message="用户名不能为空")
    @Length(min = 2, max = 50, message = "用户名长度为2-50位")
    private String username;

    @NotEmpty(message="姓名不能为空")
    @Length(min = 2, max = 50, message = "姓名长度为2-50位")
    private String name;

    @NotEmpty(message="Email不能为空")
    private String email;

    @NotEmpty(message="")
    private String password;

    private String status;

    private Long createTime;

    private Long updateTime;

    private String language;

    private String lastWorkspaceId;

    private String lastOrganizationId;

    @NotEmpty
    @Length(min = 11,max = 11,message = "手机号错误")
    private String phone;

    private String source;

    private static final long serialVersionUID = 1L;
}