package com.DigitalBank.backend.email.templates;

public class EmailTemplates {
    public static String approvedTemplate(String name) {
    return String.format("""
        <!DOCTYPE html>
        <html>
        <body style="font-family: 'Segoe UI', Arial; background-color: #f4f6f8; padding: 20px;">
            
            <div style="max-width: 500px; margin: auto; background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.1);">

                <div style="background: linear-gradient(90deg, #4CAF50, #2e7d32); padding: 20px; color: white;">
                    <h2 style="margin: 0;">DigitalBank</h2>
                    <p style="margin: 0; font-size: 14px;">Notificación de cuenta</p>
                </div>

                <div style="padding: 20px;">
                    <h3 style="color: #2e7d32;"> Cuenta aprobada</h3>

                    <p>Hola <strong>%s</strong>,</p>

                    <p>Tu cuenta ha sido validada exitosamente. Ya puedes acceder a nuestros servicios.</p>

                    <div style="margin: 20px 0; padding: 15px; background-color: #e8f5e9; border-radius: 8px;">
                         Bienvenido a DigitalBank
                    </div>

                
                </div>

                <div style="background-color: #f1f1f1; padding: 10px; text-align: center; font-size: 12px; color: gray;">
                    Este es un mensaje automático
                </div>

            </div>

        </body>
        </html>
    """, name);
}

    public static String rejectedTemplate(String name, String comment) {
    return String.format("""
        <!DOCTYPE html>
        <html>
        <body style="font-family: 'Segoe UI', Arial; background-color: #f4f6f8; padding: 20px;">
            
            <div style="max-width: 500px; margin: auto; background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.1);">

                <div style="background: linear-gradient(90deg, #e53935, #c62828); padding: 20px; color: white;">
                    <h2 style="margin: 0;">DigitalBank</h2>
                    <p style="margin: 0; font-size: 14px;">Notificación de cuenta</p>
                </div>

                <div style="padding: 20px;">
                    <h3 style="color: #c62828;"> Documentación rechazada</h3>

                    <p>Hola <strong>%s</strong>,</p>

                    <p>No pudimos validar tu identidad. Por favor revisa el siguiente motivo:</p>

                    <div style="margin: 20px 0; padding: 15px; background-color: #ffebee; border-radius: 8px; color: #c62828;">
                        %s
                    </div>

                    <p>Puedes volver a subir la documentación desde la plataforma.</p>
                </div>

                <div style="background-color: #f1f1f1; padding: 10px; text-align: center; font-size: 12px; color: gray;">
                    Este es un mensaje automático
                </div>

            </div>

        </body>
        </html>
    """, name, comment);
}
}
