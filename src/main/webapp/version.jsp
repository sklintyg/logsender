<%@ page language="java" contentType="text/html; charset=UTF-8" session="false" pageEncoding="UTF-8"
         trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="sv">
<head>

<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=Edge" />
<meta name="ROBOTS" content="nofollow, noindex" />
<meta name="viewport" content="width=device-width, initial-scale=1">

<title>LogSender Version</title>

</head>
<body style="padding: 10px;">
  <div class="page-header" style="margin-top: 0;">
    <h3>Version</h3>
  </div>
  <div class="alert alert-info">
    <h4>Configuration info</h4>
    <div>
      Application version:
      <span class="label label-info"><spring:message code="project.version"></spring:message></span>
    </div>
    <div>
      Build number:
      <span class="label label-info"><spring:message code="buildNumber"></spring:message></span>
    </div>
    <div>
      Build time:
      <span class="label label-info"><spring:message code="buildTime"></spring:message></span>
    </div>
    <div>
      Spring profiles:
      <div>
        <span style="margin-left: 20px;">From SYSTEM (Primary):</span> <span class="label label-info"><%= System.getProperty("spring.profiles.active") %></span><br/>
        <span style="margin-left: 20px;">From ENV (Secondary):</span> <span class="label label-info"><%= System.getenv("SPRING_PROFILES_ACTIVE") %></span>
      </div>
    </div>
  </div>

</body>
</html>
