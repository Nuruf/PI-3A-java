package service.api;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class EmailService {

    private final Session session;
    private final boolean enabled;

    public EmailService() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", ApiConfig.SMTP_HOST);
        props.put("mail.smtp.port", ApiConfig.SMTP_PORT);
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.ssl.trust", ApiConfig.SMTP_HOST);
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        this.session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                        ApiConfig.SMTP_USERNAME,
                        ApiConfig.SMTP_PASSWORD
                );
            }
        });

        // Enable debug mode for troubleshooting
        this.session.setDebug(false); // Set to true for debugging

        this.enabled = ApiConfig.SMTP_USERNAME != null
                && !ApiConfig.SMTP_USERNAME.isEmpty()
                && !ApiConfig.SMTP_USERNAME.equals("your_email@gmail.com")
                && ApiConfig.SMTP_PASSWORD != null
                && !ApiConfig.SMTP_PASSWORD.isEmpty();

        System.out.println("═══════════════════════════════════════════════");
        System.out.println("📧 EMAIL SERVICE INITIALIZATION");
        System.out.println("   Status: " + (enabled ? "✅ ENABLED" : "❌ DISABLED"));
        System.out.println("   SMTP Host: " + ApiConfig.SMTP_HOST);
        System.out.println("   SMTP Port: " + ApiConfig.SMTP_PORT);
        System.out.println("   Username: " + (ApiConfig.SMTP_USERNAME != null ?
                ApiConfig.SMTP_USERNAME.substring(0, Math.min(5, ApiConfig.SMTP_USERNAME.length())) + "***" : "null"));
        System.out.println("═══════════════════════════════════════════════");
    }

    public boolean isEnabled() {
        return enabled;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CORE SEND EMAIL
    // ═══════════════════════════════════════════════════════════════════════

    private boolean sendEmail(String toEmail, String toName, String subject, String htmlBody) {
        if (!enabled) {
            System.out.println("⚠️ [SIMULATION] Email would be sent to: " + toEmail);
            System.out.println("   Subject: " + subject);
            return true;
        }

        if (toEmail == null || toEmail.isEmpty() || !toEmail.contains("@")) {
            System.err.println("❌ Invalid email address: " + toEmail);
            return false;
        }

        try {
            System.out.println("📧 Sending email to: " + toEmail);

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(ApiConfig.SMTP_USERNAME, ApiConfig.SMTP_FROM_NAME));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail, toName));
            message.setSubject(subject, "UTF-8");
            message.setContent(htmlBody, "text/html; charset=UTF-8");
            message.setSentDate(new java.util.Date());

            Transport.send(message);

            System.out.println("✅ Email sent successfully to: " + toEmail);
            return true;

        } catch (AuthenticationFailedException e) {
            System.err.println("❌ EMAIL AUTHENTICATION FAILED!");
            System.err.println("   Check your SMTP_USERNAME and SMTP_PASSWORD in ApiConfig.java");
            System.err.println("   For Gmail: Use App Password, not regular password");
            System.err.println("   Error: " + e.getMessage());
            return false;

        } catch (MessagingException e) {
            System.err.println("❌ EMAIL SENDING FAILED to " + toEmail);
            System.err.println("   Error: " + e.getMessage());
            e.printStackTrace();
            return false;

        } catch (Exception e) {
            System.err.println("❌ UNEXPECTED EMAIL ERROR: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // NEW DEMANDE → RH
    // ═══════════════════════════════════════════════════════════════════════

    public CompletableFuture<Boolean> sendNewDemandeToRH(
            String employeeName, String employeeEmail,
            String demandeTitre, String demandeType, String priorite) {

        return CompletableFuture.supplyAsync(() -> {
            String subject = "📋 Nouvelle Demande: " + demandeTitre;

            String prioriteColor;
            switch (priorite != null ? priorite.toUpperCase() : "") {
                case "HAUTE": prioriteColor = "#e74c3c"; break;
                case "NORMALE": prioriteColor = "#3498db"; break;
                default: prioriteColor = "#27ae60";
            }

            String html = buildNewDemandeHtml(employeeName, employeeEmail, demandeTitre, demandeType, priorite, prioriteColor);

            return sendEmail(ApiConfig.RH_EMAIL, "Service RH", subject, html);
        });
    }

    private String buildNewDemandeHtml(String employeeName, String employeeEmail,
                                       String demandeTitre, String demandeType, String priorite, String prioriteColor) {
        return "<!DOCTYPE html><html><body style='font-family:Segoe UI,Arial,sans-serif;margin:0;padding:0;background:#f4f4f4'>"
                + "<div style='max-width:600px;margin:20px auto;background:white;border-radius:12px;overflow:hidden;box-shadow:0 4px 15px rgba(0,0,0,0.1)'>"
                + "  <div style='background:linear-gradient(135deg,#2c3e50,#3498db);padding:30px;text-align:center'>"
                + "    <h1 style='color:white;margin:0;font-size:24px'>📋 Nouvelle Demande</h1>"
                + "    <p style='color:#bdc3c7;margin:5px 0 0'>Système de Gestion des Demandes</p>"
                + "  </div>"
                + "  <div style='padding:30px'>"
                + "    <div style='background:#f8f9fa;border-left:4px solid #3498db;padding:15px;border-radius:0 8px 8px 0;margin-bottom:20px'>"
                + "      <p style='margin:0;color:#555'>Une nouvelle demande a été soumise par <strong>" + escapeHtml(employeeName) + "</strong></p>"
                + "    </div>"
                + "    <table style='width:100%;border-collapse:collapse'>"
                + "      <tr><td style='padding:12px;border-bottom:1px solid #eee;color:#888;width:140px'>📌 Titre</td>"
                + "          <td style='padding:12px;border-bottom:1px solid #eee;font-weight:bold'>" + escapeHtml(demandeTitre) + "</td></tr>"
                + "      <tr><td style='padding:12px;border-bottom:1px solid #eee;color:#888'>📂 Type</td>"
                + "          <td style='padding:12px;border-bottom:1px solid #eee'>" + escapeHtml(demandeType) + "</td></tr>"
                + "      <tr><td style='padding:12px;border-bottom:1px solid #eee;color:#888'>⚡ Priorité</td>"
                + "          <td style='padding:12px;border-bottom:1px solid #eee'>"
                + "            <span style='background:" + prioriteColor + ";color:white;padding:4px 12px;border-radius:12px;font-size:13px'>" + escapeHtml(priorite) + "</span></td></tr>"
                + "      <tr><td style='padding:12px;border-bottom:1px solid #eee;color:#888'>👤 Employé</td>"
                + "          <td style='padding:12px;border-bottom:1px solid #eee'>" + escapeHtml(employeeName) + "</td></tr>"
                + "      <tr><td style='padding:12px;color:#888'>📧 Email</td>"
                + "          <td style='padding:12px'><a href='mailto:" + escapeHtml(employeeEmail) + "'>" + escapeHtml(employeeEmail) + "</a></td></tr>"
                + "    </table>"
                + "  </div>"
                + "  <div style='background:#f8f9fa;padding:15px;text-align:center;color:#999;font-size:12px'>"
                + "    " + ApiConfig.COMPANY_NAME + " — Système RH"
                + "  </div>"
                + "</div></body></html>";
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STATUS CHANGE → EMPLOYEE
    // ═══════════════════════════════════════════════════════════════════════

    public CompletableFuture<Boolean> sendStatusChangeToEmployee(
            String employeeName, String employeeEmail,
            String demandeTitre,
            String oldStatus, String newStatus,
            String acteur, String comment) {

        return CompletableFuture.supplyAsync(() -> {
            System.out.println("═══════════════════════════════════════════════");
            System.out.println("📧 PREPARING STATUS CHANGE EMAIL");
            System.out.println("   To: " + employeeEmail);
            System.out.println("   Name: " + employeeName);
            System.out.println("   Demande: " + demandeTitre);
            System.out.println("   Status: " + oldStatus + " → " + newStatus);
            System.out.println("═══════════════════════════════════════════════");

            if (employeeEmail == null || employeeEmail.isEmpty() || !employeeEmail.contains("@")) {
                System.err.println("❌ Invalid employee email address");
                return false;
            }

            String subject = "🔄 Mise à jour: " + demandeTitre;
            String html = buildStatusChangeHtml(employeeName, demandeTitre, oldStatus, newStatus, acteur, comment);

            return sendEmail(employeeEmail, employeeName, subject, html);
        });
    }

    private String buildStatusChangeHtml(String employeeName, String demandeTitre,
                                         String oldStatus, String newStatus, String acteur, String comment) {
        return "<!DOCTYPE html><html><body style='font-family:Segoe UI,Arial,sans-serif;margin:0;padding:0;background:#f4f4f4'>"
                + "<div style='max-width:600px;margin:20px auto;background:white;border-radius:12px;overflow:hidden;box-shadow:0 4px 15px rgba(0,0,0,0.1)'>"
                + "  <div style='background:linear-gradient(135deg,#27ae60,#2ecc71);padding:30px;text-align:center'>"
                + "    <h1 style='color:white;margin:0;font-size:24px'>🔄 Mise à jour de votre demande</h1>"
                + "  </div>"
                + "  <div style='padding:30px'>"
                + "    <p style='font-size:16px'>Bonjour <strong>" + escapeHtml(employeeName) + "</strong>,</p>"
                + "    <p style='color:#555'>Le statut de votre demande a été mis à jour :</p>"
                + "    <div style='text-align:center;margin:30px 0'>"
                + "      <span style='background:#fadbd8;color:#c0392b;padding:10px 20px;border-radius:20px;font-weight:bold;font-size:14px'>" + escapeHtml(oldStatus) + "</span>"
                + "      <span style='font-size:28px;margin:0 20px;color:#555'>→</span>"
                + "      <span style='background:#d5f5e3;color:#27ae60;padding:10px 20px;border-radius:20px;font-weight:bold;font-size:14px'>" + escapeHtml(newStatus) + "</span>"
                + "    </div>"
                + "    <table style='width:100%;border-collapse:collapse;margin-top:20px'>"
                + "      <tr><td style='padding:12px;border-bottom:1px solid #eee;color:#888;width:140px'>📌 Demande</td>"
                + "          <td style='padding:12px;border-bottom:1px solid #eee;font-weight:bold'>" + escapeHtml(demandeTitre) + "</td></tr>"
                + "      <tr><td style='padding:12px;border-bottom:1px solid #eee;color:#888'>👤 Traité par</td>"
                + "          <td style='padding:12px;border-bottom:1px solid #eee'>" + escapeHtml(acteur) + "</td></tr>"
                + "      <tr><td style='padding:12px;color:#888'>💬 Commentaire</td>"
                + "          <td style='padding:12px'>" + (comment != null && !comment.isEmpty() ? escapeHtml(comment) : "<em style='color:#999'>Aucun commentaire</em>") + "</td></tr>"
                + "    </table>"
                + "    <div style='margin-top:30px;padding:20px;background:#f8f9fa;border-radius:8px;text-align:center'>"
                + "      <p style='margin:0;color:#666'>Vous pouvez consulter votre demande dans l'application.</p>"
                + "    </div>"
                + "  </div>"
                + "  <div style='background:#f8f9fa;padding:15px;text-align:center;color:#999;font-size:12px'>"
                + "    " + ApiConfig.COMPANY_NAME + " — Système RH | " + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date())
                + "  </div>"
                + "</div></body></html>";
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DEMANDE CANCELLED → RH (Employee cancels their demande)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Send notification to RH when an employee cancels their demande
     */
    public CompletableFuture<Boolean> sendDemandeCancelledToRH(
            String employeeName,
            String employeeEmail,
            String demandeTitre,
            String demandeType,
            String demandePriorite) {

        return CompletableFuture.supplyAsync(() -> {
            System.out.println("═══════════════════════════════════════════════");
            System.out.println("📧 SENDING CANCELLATION NOTIFICATION TO RH");
            System.out.println("   Employee: " + employeeName);
            System.out.println("   Email: " + employeeEmail);
            System.out.println("   Demande: " + demandeTitre);
            System.out.println("   Type: " + demandeType);
            System.out.println("   Priorité: " + demandePriorite);
            System.out.println("═══════════════════════════════════════════════");

            String subject = "🚫 Demande Annulée: " + demandeTitre;

            String html = buildCancellationEmailForRH(
                    employeeName,
                    employeeEmail,
                    demandeTitre,
                    demandeType,
                    demandePriorite
            );

            return sendEmail(ApiConfig.RH_EMAIL, "Service RH", subject, html);
        });
    }

    /**
     * Build HTML email content for RH cancellation notification
     */
    private String buildCancellationEmailForRH(
            String employeeName,
            String employeeEmail,
            String demandeTitre,
            String demandeType,
            String demandePriorite) {

        String prioriteColor = "#3498db";
        String prioriteEmoji = "🔵";
        if ("HAUTE".equalsIgnoreCase(demandePriorite)) {
            prioriteColor = "#e74c3c";
            prioriteEmoji = "🔴";
        } else if ("BASSE".equalsIgnoreCase(demandePriorite)) {
            prioriteColor = "#27ae60";
            prioriteEmoji = "🟢";
        }

        String currentDate = new java.text.SimpleDateFormat("dd/MM/yyyy à HH:mm").format(new java.util.Date());

        return "<!DOCTYPE html>"
                + "<html>"
                + "<head><meta charset='UTF-8'></head>"
                + "<body style='font-family:Segoe UI,Arial,sans-serif;margin:0;padding:0;background:#f4f4f4'>"
                + "<div style='max-width:600px;margin:20px auto;background:white;border-radius:12px;overflow:hidden;box-shadow:0 4px 15px rgba(0,0,0,0.1)'>"

                // Header - Red gradient for cancellation
                + "  <div style='background:linear-gradient(135deg,#e74c3c,#c0392b);padding:30px;text-align:center'>"
                + "    <div style='font-size:48px;margin-bottom:10px'>🚫</div>"
                + "    <h1 style='color:white;margin:0;font-size:24px'>Demande Annulée</h1>"
                + "    <p style='color:rgba(255,255,255,0.8);margin:5px 0 0;font-size:14px'>Notification automatique</p>"
                + "  </div>"

                // Content
                + "  <div style='padding:30px'>"

                // Alert box
                + "    <div style='background:#fff5f5;border-left:4px solid #e74c3c;padding:15px;border-radius:0 8px 8px 0;margin-bottom:25px'>"
                + "      <p style='margin:0;color:#c0392b;font-weight:bold'>⚠️ Information</p>"
                + "      <p style='margin:8px 0 0;color:#555'>Un employé a annulé sa demande avant traitement.</p>"
                + "    </div>"

                // Demande details card
                + "    <div style='background:#f8f9fa;padding:20px;border-radius:8px;margin-bottom:20px'>"
                + "      <h3 style='margin:0 0 15px;color:#2c3e50;font-size:16px'>📋 Détails de la demande annulée</h3>"
                + "      <table style='width:100%;border-collapse:collapse'>"
                + "        <tr>"
                + "          <td style='padding:10px 0;border-bottom:1px solid #eee;color:#888;width:130px'>📌 Titre</td>"
                + "          <td style='padding:10px 0;border-bottom:1px solid #eee;font-weight:bold;color:#2c3e50'>" + escapeHtml(demandeTitre) + "</td>"
                + "        </tr>"
                + "        <tr>"
                + "          <td style='padding:10px 0;border-bottom:1px solid #eee;color:#888'>📂 Type</td>"
                + "          <td style='padding:10px 0;border-bottom:1px solid #eee;color:#2c3e50'>" + escapeHtml(demandeType) + "</td>"
                + "        </tr>"
                + "        <tr>"
                + "          <td style='padding:10px 0;border-bottom:1px solid #eee;color:#888'>⚡ Priorité</td>"
                + "          <td style='padding:10px 0;border-bottom:1px solid #eee'>"
                + "            <span style='background:" + prioriteColor + "20;color:" + prioriteColor + ";padding:4px 12px;border-radius:12px;font-size:12px;font-weight:bold'>"
                + "              " + prioriteEmoji + " " + escapeHtml(demandePriorite)
                + "            </span>"
                + "          </td>"
                + "        </tr>"
                + "        <tr>"
                + "          <td style='padding:10px 0;color:#888'>🚫 Statut</td>"
                + "          <td style='padding:10px 0'>"
                + "            <span style='background:#fadbd8;color:#c0392b;padding:4px 12px;border-radius:12px;font-size:12px;font-weight:bold'>Annulée</span>"
                + "          </td>"
                + "        </tr>"
                + "      </table>"
                + "    </div>"

                // Employee info card
                + "    <div style='background:#f8f9fa;padding:20px;border-radius:8px;margin-bottom:20px'>"
                + "      <h3 style='margin:0 0 15px;color:#2c3e50;font-size:16px'>👤 Informations employé</h3>"
                + "      <table style='width:100%;border-collapse:collapse'>"
                + "        <tr>"
                + "          <td style='padding:10px 0;border-bottom:1px solid #eee;color:#888;width:130px'>👤 Nom</td>"
                + "          <td style='padding:10px 0;border-bottom:1px solid #eee;color:#2c3e50;font-weight:bold'>" + escapeHtml(employeeName) + "</td>"
                + "        </tr>"
                + "        <tr>"
                + "          <td style='padding:10px 0;border-bottom:1px solid #eee;color:#888'>📧 Email</td>"
                + "          <td style='padding:10px 0;border-bottom:1px solid #eee'>"
                + "            <a href='mailto:" + escapeHtml(employeeEmail) + "' style='color:#3498db;text-decoration:none'>" + escapeHtml(employeeEmail) + "</a>"
                + "          </td>"
                + "        </tr>"
                + "        <tr>"
                + "          <td style='padding:10px 0;color:#888'>🕐 Date annulation</td>"
                + "          <td style='padding:10px 0;color:#2c3e50'>" + currentDate + "</td>"
                + "        </tr>"
                + "      </table>"
                + "    </div>"

                // Note
                + "    <div style='background:#e8f6f3;padding:15px;border-radius:8px;border-left:4px solid #27ae60'>"
                + "      <p style='margin:0;color:#27ae60;font-size:13px'>"
                + "        💡 <em>Cette demande a été annulée par l'employé car elle n'avait pas encore été traitée. "
                + "        Aucune action n'est requise de votre part.</em>"
                + "      </p>"
                + "    </div>"

                + "  </div>"

                // Footer
                + "  <div style='background:#f8f9fa;padding:20px;text-align:center;border-top:1px solid #eee'>"
                + "    <p style='margin:0;color:#999;font-size:12px'>📧 Email automatique du système de gestion des demandes</p>"
                + "    <p style='margin:5px 0 0;color:#bbb;font-size:11px'>" + ApiConfig.COMPANY_NAME + " — Système RH</p>"
                + "  </div>"

                + "</div>"
                + "</body>"
                + "</html>";
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DEMANDE CANCELLED → EMPLOYEE (Confirmation to employee)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Send confirmation to employee when they cancel their demande
     */
    public CompletableFuture<Boolean> sendDemandeCancelledConfirmation(
            String employeeName,
            String employeeEmail,
            String demandeTitre,
            String demandeType) {

        return CompletableFuture.supplyAsync(() -> {
            System.out.println("═══════════════════════════════════════════════");
            System.out.println("📧 SENDING CANCELLATION CONFIRMATION TO EMPLOYEE");
            System.out.println("   Employee: " + employeeName);
            System.out.println("   Email: " + employeeEmail);
            System.out.println("   Demande: " + demandeTitre);
            System.out.println("═══════════════════════════════════════════════");

            if (employeeEmail == null || employeeEmail.isEmpty() || !employeeEmail.contains("@")) {
                System.err.println("❌ Invalid employee email: " + employeeEmail);
                return false;
            }

            String subject = "✅ Confirmation d'annulation: " + demandeTitre;

            String html = buildCancellationConfirmationEmail(
                    employeeName,
                    demandeTitre,
                    demandeType
            );

            return sendEmail(employeeEmail, employeeName, subject, html);
        });
    }

    /**
     * Build HTML email content for employee cancellation confirmation
     */
    private String buildCancellationConfirmationEmail(
            String employeeName,
            String demandeTitre,
            String demandeType) {

        String currentDate = new java.text.SimpleDateFormat("dd/MM/yyyy à HH:mm").format(new java.util.Date());

        return "<!DOCTYPE html>"
                + "<html>"
                + "<head><meta charset='UTF-8'></head>"
                + "<body style='font-family:Segoe UI,Arial,sans-serif;margin:0;padding:0;background:#f4f4f4'>"
                + "<div style='max-width:600px;margin:20px auto;background:white;border-radius:12px;overflow:hidden;box-shadow:0 4px 15px rgba(0,0,0,0.1)'>"

                // Header - Green gradient for confirmation
                + "  <div style='background:linear-gradient(135deg,#27ae60,#2ecc71);padding:30px;text-align:center'>"
                + "    <div style='font-size:48px;margin-bottom:10px'>✅</div>"
                + "    <h1 style='color:white;margin:0;font-size:24px'>Annulation Confirmée</h1>"
                + "    <p style='color:rgba(255,255,255,0.8);margin:5px 0 0;font-size:14px'>Votre demande a été annulée</p>"
                + "  </div>"

                // Content
                + "  <div style='padding:30px'>"

                // Greeting
                + "    <p style='font-size:16px;color:#2c3e50;margin-bottom:20px'>"
                + "      Bonjour <strong>" + escapeHtml(employeeName) + "</strong>,"
                + "    </p>"

                // Success box
                + "    <div style='background:#d5f5e3;border-left:4px solid #27ae60;padding:15px;border-radius:0 8px 8px 0;margin-bottom:25px'>"
                + "      <p style='margin:0;color:#27ae60;font-weight:bold'>✅ Confirmation</p>"
                + "      <p style='margin:8px 0 0;color:#555'>Votre demande a été annulée avec succès.</p>"
                + "    </div>"

                // Recap card
                + "    <div style='background:#f8f9fa;padding:20px;border-radius:8px;margin-bottom:20px'>"
                + "      <h3 style='margin:0 0 15px;color:#2c3e50;font-size:16px'>📋 Récapitulatif</h3>"
                + "      <table style='width:100%;border-collapse:collapse'>"
                + "        <tr>"
                + "          <td style='padding:10px 0;border-bottom:1px solid #eee;color:#888;width:130px'>📌 Titre</td>"
                + "          <td style='padding:10px 0;border-bottom:1px solid #eee;font-weight:bold;color:#2c3e50'>" + escapeHtml(demandeTitre) + "</td>"
                + "        </tr>"
                + "        <tr>"
                + "          <td style='padding:10px 0;border-bottom:1px solid #eee;color:#888'>📂 Type</td>"
                + "          <td style='padding:10px 0;border-bottom:1px solid #eee;color:#2c3e50'>" + escapeHtml(demandeType) + "</td>"
                + "        </tr>"
                + "        <tr>"
                + "          <td style='padding:10px 0;border-bottom:1px solid #eee;color:#888'>🚫 Statut</td>"
                + "          <td style='padding:10px 0;border-bottom:1px solid #eee'>"
                + "            <span style='background:#fadbd8;color:#c0392b;padding:4px 12px;border-radius:12px;font-size:12px;font-weight:bold'>Annulée</span>"
                + "          </td>"
                + "        </tr>"
                + "        <tr>"
                + "          <td style='padding:10px 0;color:#888'>🕐 Date</td>"
                + "          <td style='padding:10px 0;color:#2c3e50'>" + currentDate + "</td>"
                + "        </tr>"
                + "      </table>"
                + "    </div>"

                // Info note
                + "    <div style='background:#e8f4fd;padding:15px;border-radius:8px;border-left:4px solid #3498db;margin-bottom:20px'>"
                + "      <p style='margin:0;color:#2980b9;font-size:13px'>"
                + "        ℹ️ Le service RH a été notifié de cette annulation."
                + "      </p>"
                + "    </div>"

                // New demande suggestion
                + "    <div style='text-align:center;padding:20px;background:#f8f9fa;border-radius:8px'>"
                + "      <p style='margin:0 0 10px;color:#666;font-size:14px'>Besoin de soumettre une nouvelle demande?</p>"
                + "      <p style='margin:0;color:#888;font-size:12px'>Connectez-vous à votre espace employé pour créer une nouvelle demande.</p>"
                + "    </div>"

                + "  </div>"

                // Footer
                + "  <div style='background:#f8f9fa;padding:20px;text-align:center;border-top:1px solid #eee'>"
                + "    <p style='margin:0;color:#999;font-size:12px'>📧 Email automatique - Ne pas répondre</p>"
                + "    <p style='margin:5px 0 0;color:#bbb;font-size:11px'>" + ApiConfig.COMPANY_NAME + " — Système RH</p>"
                + "  </div>"

                + "</div>"
                + "</body>"
                + "</html>";
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DEMANDE CREATED CONFIRMATION → EMPLOYEE
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Send confirmation to employee when they create a new demande
     */
    public CompletableFuture<Boolean> sendDemandeCreatedConfirmation(
            String employeeName,
            String employeeEmail,
            String demandeTitre,
            String demandeType,
            String demandePriorite) {

        return CompletableFuture.supplyAsync(() -> {
            System.out.println("═══════════════════════════════════════════════");
            System.out.println("📧 SENDING DEMANDE CREATED CONFIRMATION");
            System.out.println("   Employee: " + employeeName);
            System.out.println("   Email: " + employeeEmail);
            System.out.println("   Demande: " + demandeTitre);
            System.out.println("═══════════════════════════════════════════════");

            if (employeeEmail == null || employeeEmail.isEmpty() || !employeeEmail.contains("@")) {
                System.err.println("❌ Invalid employee email: " + employeeEmail);
                return false;
            }

            String subject = "📋 Demande soumise: " + demandeTitre;

            String html = buildDemandeCreatedConfirmationEmail(
                    employeeName,
                    demandeTitre,
                    demandeType,
                    demandePriorite
            );

            return sendEmail(employeeEmail, employeeName, subject, html);
        });
    }

    private String buildDemandeCreatedConfirmationEmail(
            String employeeName,
            String demandeTitre,
            String demandeType,
            String demandePriorite) {

        String prioriteColor = "#3498db";
        if ("HAUTE".equalsIgnoreCase(demandePriorite)) {
            prioriteColor = "#e74c3c";
        } else if ("BASSE".equalsIgnoreCase(demandePriorite)) {
            prioriteColor = "#27ae60";
        }

        String currentDate = new java.text.SimpleDateFormat("dd/MM/yyyy à HH:mm").format(new java.util.Date());

        return "<!DOCTYPE html>"
                + "<html>"
                + "<head><meta charset='UTF-8'></head>"
                + "<body style='font-family:Segoe UI,Arial,sans-serif;margin:0;padding:0;background:#f4f4f4'>"
                + "<div style='max-width:600px;margin:20px auto;background:white;border-radius:12px;overflow:hidden;box-shadow:0 4px 15px rgba(0,0,0,0.1)'>"

                // Header
                + "  <div style='background:linear-gradient(135deg,#3498db,#2980b9);padding:30px;text-align:center'>"
                + "    <div style='font-size:48px;margin-bottom:10px'>📋</div>"
                + "    <h1 style='color:white;margin:0;font-size:24px'>Demande Soumise</h1>"
                + "    <p style='color:rgba(255,255,255,0.8);margin:5px 0 0;font-size:14px'>Votre demande a été enregistrée</p>"
                + "  </div>"

                // Content
                + "  <div style='padding:30px'>"
                + "    <p style='font-size:16px;color:#2c3e50;margin-bottom:20px'>"
                + "      Bonjour <strong>" + escapeHtml(employeeName) + "</strong>,"
                + "    </p>"

                + "    <div style='background:#d4edfc;border-left:4px solid #3498db;padding:15px;border-radius:0 8px 8px 0;margin-bottom:25px'>"
                + "      <p style='margin:0;color:#2980b9;font-weight:bold'>📝 Demande enregistrée</p>"
                + "      <p style='margin:8px 0 0;color:#555'>Votre demande a été soumise avec succès et sera traitée par le service RH.</p>"
                + "    </div>"

                + "    <div style='background:#f8f9fa;padding:20px;border-radius:8px;margin-bottom:20px'>"
                + "      <h3 style='margin:0 0 15px;color:#2c3e50;font-size:16px'>📋 Récapitulatif</h3>"
                + "      <table style='width:100%;border-collapse:collapse'>"
                + "        <tr>"
                + "          <td style='padding:10px 0;border-bottom:1px solid #eee;color:#888;width:130px'>📌 Titre</td>"
                + "          <td style='padding:10px 0;border-bottom:1px solid #eee;font-weight:bold;color:#2c3e50'>" + escapeHtml(demandeTitre) + "</td>"
                + "        </tr>"
                + "        <tr>"
                + "          <td style='padding:10px 0;border-bottom:1px solid #eee;color:#888'>📂 Type</td>"
                + "          <td style='padding:10px 0;border-bottom:1px solid #eee;color:#2c3e50'>" + escapeHtml(demandeType) + "</td>"
                + "        </tr>"
                + "        <tr>"
                + "          <td style='padding:10px 0;border-bottom:1px solid #eee;color:#888'>⚡ Priorité</td>"
                + "          <td style='padding:10px 0;border-bottom:1px solid #eee'>"
                + "            <span style='background:" + prioriteColor + ";color:white;padding:4px 12px;border-radius:12px;font-size:12px;font-weight:bold'>" + escapeHtml(demandePriorite) + "</span>"
                + "          </td>"
                + "        </tr>"
                + "        <tr>"
                + "          <td style='padding:10px 0;border-bottom:1px solid #eee;color:#888'>📊 Statut</td>"
                + "          <td style='padding:10px 0;border-bottom:1px solid #eee'>"
                + "            <span style='background:#3498db;color:white;padding:4px 12px;border-radius:12px;font-size:12px;font-weight:bold'>Nouvelle</span>"
                + "          </td>"
                + "        </tr>"
                + "        <tr>"
                + "          <td style='padding:10px 0;color:#888'>🕐 Date</td>"
                + "          <td style='padding:10px 0;color:#2c3e50'>" + currentDate + "</td>"
                + "        </tr>"
                + "      </table>"
                + "    </div>"

                + "    <div style='background:#fff8e6;padding:15px;border-radius:8px;border-left:4px solid #f39c12'>"
                + "      <p style='margin:0;color:#d68910;font-size:13px'>"
                + "        ⏳ <em>Vous recevrez une notification par email lorsque le statut de votre demande sera mis à jour.</em>"
                + "      </p>"
                + "    </div>"

                + "  </div>"

                + "  <div style='background:#f8f9fa;padding:20px;text-align:center;border-top:1px solid #eee'>"
                + "    <p style='margin:0;color:#999;font-size:12px'>📧 Email automatique - Ne pas répondre</p>"
                + "    <p style='margin:5px 0 0;color:#bbb;font-size:11px'>" + ApiConfig.COMPANY_NAME + " — Système RH</p>"
                + "  </div>"

                + "</div>"
                + "</body>"
                + "</html>";
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Escape HTML special characters to prevent XSS
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TEST CONNECTION
    // ═══════════════════════════════════════════════════════════════════════

    public void testConnection() {
        System.out.println("═══════════════════════════════════════════════");
        System.out.println("📧 SMTP EMAIL CONFIGURATION TEST");
        System.out.println("   Enabled:  " + enabled);
        System.out.println("   Host:     " + ApiConfig.SMTP_HOST);
        System.out.println("   Port:     " + ApiConfig.SMTP_PORT);
        System.out.println("   Username: " + ApiConfig.SMTP_USERNAME);
        System.out.println("   RH Email: " + ApiConfig.RH_EMAIL);
        System.out.println("═══════════════════════════════════════════════");

        if (enabled) {
            System.out.println("\n🔄 Sending test email...");
            boolean ok = sendEmail(
                    ApiConfig.RH_EMAIL,
                    "Test",
                    "✅ Test Email - SMTP Works!",
                    "<div style='font-family:Arial;padding:20px'>" +
                            "<h2 style='color:#27ae60'>✅ Email Service Working!</h2>" +
                            "<p>Timestamp: " + new java.util.Date() + "</p>" +
                            "<p>Your email configuration is correct.</p>" +
                            "</div>"
            );
            System.out.println(ok ? "✅ Test email sent successfully!" : "❌ Test email failed!");
        } else {
            System.out.println("\n⚠️ Email service is disabled. Update ApiConfig.java with valid credentials.");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // QUICK TEST METHOD
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Quick test to verify email sending works
     */
    public CompletableFuture<Boolean> sendTestEmail(String toEmail) {
        return CompletableFuture.supplyAsync(() -> {
            String subject = "🧪 Test Email - " + new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
            String html = "<!DOCTYPE html>"
                    + "<html><body style='font-family:Arial,sans-serif;padding:20px'>"
                    + "<h2 style='color:#27ae60'>✅ Test Email Successful!</h2>"
                    + "<p>This is a test email from the HR System.</p>"
                    + "<p><strong>Timestamp:</strong> " + new java.util.Date() + "</p>"
                    + "<p><strong>Sent to:</strong> " + toEmail + "</p>"
                    + "<hr style='border:1px solid #eee'>"
                    + "<p style='color:#888;font-size:12px'>" + ApiConfig.COMPANY_NAME + " — Email Service Test</p>"
                    + "</body></html>";

            return sendEmail(toEmail, "Test User", subject, html);
        });
    }
}