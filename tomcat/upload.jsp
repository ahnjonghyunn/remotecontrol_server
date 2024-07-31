<%@ page import="java.io.*, javax.servlet.*, javax.servlet.http.*, java.util.*" %>
<%
    // 업로드된 파일들이 저장될 경로를 설정합니다.
    String savePath = application.getRealPath("/") + "uploads";
    File uploadDir = new File(savePath);

    // uploads 디렉토리가 존재하지 않으면 생성합니다.
    if (!uploadDir.exists()) {
        uploadDir.mkdir();
    }

    // 클라이언트로부터 전송된 파일 이름을 헤더에서 가져옵니다.
    String fileName = request.getHeader("filename");

    // 파일 이름이 제공되지 않았거나 비어있는 경우, 기본 파일 이름을 설정합니다.
    if (fileName == null || fileName.isEmpty()) {
        fileName = "frame_" + new Date().getTime() + ".png";
    }
    // 파일 객체를 생성합니다.
    File file = new File(uploadDir, fileName);

    // 디버그 정보를 로그에 출력합니다.
    System.out.println("Save Path: " + savePath);
    System.out.println("File Name: " + fileName);

    try (InputStream input = request.getInputStream(); // 요청에서 입력 스트림을 가져옵니다.
         FileOutputStream output = new FileOutputStream(file)) { // 파일 출력 스트림을 생성합니$
        byte[] buffer = new byte[4096];
        int bytesRead;
        int totalBytesRead = 0;

        // 입력 스트림에서 데이터를 읽어와 파일에 씁니다.
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
            totalBytesRead += bytesRead;
        }

        // 총 읽어들인 바이트 수를 출력합니다.
        System.out.println("전체 바이트  Read: " + totalBytesRead);

        // 파일 크기가 0바이트인 경우 파일을 삭제합니다.
        if (totalBytesRead == 0) {
            file.delete();
            System.out.println("삭제  0-byte file: " + fileName);
        } else {
            System.out.println("파일 성공 럽로드: " + fileName);
        }
    } catch (IOException e) {
        // 예외가 발생한 경우 스택 트레이스를 출력합니다.
        e.printStackTrace();
    }

    // 업로드된 파일의 경로를 응답으로 클라이언트에 전송합니다.
    response.getWriter().write("uploads/" + fileName);
%>

<!DOCTYPE html>
<html>
<head>
    <title>Uploaded Files</title>
</head>
<body>
    <h1>Uploaded Files</h1>
    <ul>
        <%
            // 업로드된 파일 목록을 출력합니다.
            File[] files = uploadDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    out.println("<li><a href='uploads/" + f.getName() + "'>" + f.getName() + "<$
                }
            }
        %>
    </ul>
</body>
</html>
