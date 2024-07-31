
<%@ page import="java.io.*, javax.servlet.*, javax.servlet.http.*, java.util.*" %>
<%
    // 업로드된 파일들이 저장된 경로를 설정합니다.
    String uploadPath = application.getRealPath("/") + "uploads";
    File uploadDir = new File(uploadPath);

    // uploads 폴더 내의 모든 png 파일들을 가져옵니다.
    File[] files = uploadDir.listFiles(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            // 확장자가 .png인 파일들만 가져옵니다.
            return name.endsWith(".png");
        }
    });

    // 파일들을 수정된 시간 순으로 정렬합니다.
    if (files != null && files.length > 0) {
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                // 최신 파일이 첫 번째로 오도록 정렬합니다.
                return Long.compare(f2.lastModified(), f1.lastModified());
            }
        });

        // 가장 최근 파일의 이름을 반환합니다.
        String latestFileName = files[0].getName();
        response.getWriter().write(latestFileName);

        // 디버깅을 위해 파일 이름 출력
        System.out.println("Latest file: " + latestFileName);
    } else {
        // 파일이 없으면 빈 문자열을 반환합니다.
        response.getWriter().write("");
    }
%>

