package com.pfa.interviewai.rest;

import com.pfa.interviewai.rest.dto.CvAnalysisResponse;
import com.pfa.interviewai.security.JwtUtil;
import com.pfa.interviewai.service.ClaudeAIService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import java.io.InputStream;
import java.util.Map;
import java.util.logging.Logger;

@Path("/cv")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class CvRestController {

    private static final Logger log = Logger.getLogger(CvRestController.class.getName());
    private static final int MAX_BYTES = 5 * 1024 * 1024; // 5 MB

    @Inject private ClaudeAIService claudeAIService;
    @Inject private JwtUtil jwtUtil;

    @POST
    @Path("/analyze")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response analyzeCv(
            @FormDataParam("file") InputStream fileStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail,
            @CookieParam("interview_jwt") String cookieToken,
            @HeaderParam("Authorization") String authHeader) {

        String token = resolveToken(cookieToken, authHeader);
        if (token == null || !jwtUtil.isTokenValid(token)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(Map.of("error", "Authentication required")).build();
        }

        if (fileStream == null || fileDetail == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "No file uploaded")).build();
        }

        String filename = fileDetail.getFileName();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Only PDF files are accepted")).build();
        }

        byte[] fileBytes;
        try {
            fileBytes = fileStream.readNBytes(MAX_BYTES + 1);
        } catch (Exception e) {
            log.warning("CV stream read error: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Could not read uploaded file")).build();
        }

        if (fileBytes.length > MAX_BYTES) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "File too large. Maximum allowed size is 5 MB")).build();
        }

        String cvText;
        try (PDDocument doc = Loader.loadPDF(fileBytes)) {
            cvText = new PDFTextStripper().getText(doc).strip();
        } catch (Exception e) {
            log.warning("PDF extraction failed for '" + filename + "': " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Could not extract text from PDF. Ensure the file is not a scanned image")).build();
        }

        if (cvText.length() < 100) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "PDF contains no extractable text. Scanned image PDFs are not supported")).build();
        }

        try {
            CvAnalysisResponse result = claudeAIService.analyzeCv(cvText);
            return Response.ok(result).build();
        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("Anthropic API") || msg.contains("Claude API call failed")) {
                log.severe("Claude API unavailable during CV analysis: " + msg);
                return Response.status(503)
                    .entity(Map.of("error", "AI service temporarily unavailable")).build();
            }
            log.warning("CV analysis failed: " + msg);
            return Response.status(502)
                .entity(Map.of("error", "CV analysis could not produce a recommendation")).build();
        }
    }

    private String resolveToken(String cookie, String header) {
        if (cookie != null && !cookie.isBlank()) return cookie;
        if (header != null && header.startsWith("Bearer ")) return header.substring(7);
        return null;
    }
}
