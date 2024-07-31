<%@ page import="java.io.*" %>
<%
    // 업로드된 파일들이 저장된 경로를 설정합니다.
    String uploadPath = application.getRealPath("/") + "uploads";
    File uploadDir = new File(uploadPath);

    // uploads 디렉터리가 존재하는지 확인합니다.
    if (uploadDir.exists()) {
        // uploads 디렉터리 내의 모든 파일들을 가져옵니다.
        File[] files = uploadDir.listFiles();

        // 파일들이 존재하는지 확인합니다.
        if (files != null) {
            // 모든 파일들을 순회하며 삭제합니다.
            for (File file : files) {
                file.delete();
            }
        }
    }

    // 클라이언트에 업로드 디렉터리가 초기화되었음을 응답합니다.
    response.getWriter().write("Uploads directory has been reset.");
%>
