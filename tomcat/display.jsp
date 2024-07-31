<%@ page import="java.io.*, javax.servlet.*, javax.servlet.http.*, java.util.*" %>
<%
    // 업로드된 파일들이 저장된 경로를 설정합니다.
    String uploadPath = application.getRealPath("/") + "uploads";
    File uploadDir = new File(uploadPath);

    // uploads 폴더 내의 모든 png 파일들을 가져옵니다.
    File[] files = uploadDir.listFiles(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".png"); // 확장자가 .png인 파일들만 가져옵니다.
        }
    });

    // 디버깅을 위해 파일 목록을 출력합니다.
    System.out.println("Files in upload directory:");
    if (files != null) {
        // 파일 목록을 순회하며 파일 이름을 출력합니다.
        for (File file : files) {
            System.out.println(file.getName());
        }
    } else {
        // 파일이 없는 경우 메시지를 출력합니다.
        System.out.println("No files found in upload directory.");
    }

    // 파일들을 수정된 시간 순으로 정렬합니다.
    if (files != null) {
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return Long.compare(f1.lastModified(), f2.lastModified()); // 마지막 수정 시간 $
            }
        });
    }
%>

<%@ page import="java.io.*, javax.servlet.*, javax.servlet.http.*, java.util.*" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Captured Frames</title>
    <script>
        // 최신 이미지를 업데이트하는 함수
        function updateImage() {
            // AJAX 요청을 만듭니다.
            var xhr = new XMLHttpRequest();
            xhr.open('GET', 'latestImage.jsp', true); // latestImage.jsp 파일을 호출합니다.
            xhr.onload = function() {
                // 요청이 성공적으로 완료되고 응답이 비어있지 않은 경우
                if (xhr.status === 200 && xhr.responseText.trim() !== "") {
                    // 이미지의 src 속성을 최신 이미지로 업데이트합니다.
                    document.getElementById('image').src = 'uploads/' + xhr.responseText + '?' $
                }
            };
            xhr.send(); // 요청을 전송합니다.
        }

        // 5초마다 updateImage 함수를 호출하여 최신 이미지로 업데이트합니다.
        setInterval(updateImage, 5000);

        // 페이지가 로드될 때 첫 번째 이미지를 가져옵니다.
        window.onload = updateImage;
    </script>
</head>
<body>
    <h1>Captured frame will appear here</h1>
    <img id="image" src="" alt="Latest Image" />
</body>
</html>
