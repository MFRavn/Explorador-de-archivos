package shell;

import core.FileSession;

/**
 * Contrato que debe cumplir cualquier comando del gestor.
 * Al añadir nuevos comandos (copiar, mover, zip...) solo hay que
 * implementar esta interfaz y registrarlo en el Shell.
 */
public interface Command {

    /**
     * Ejecuta el comando con los argumentos dados.
     *
     * @param args    Palabras después del nombre del comando. Puede estar vacío.
     * @param session Estado actual de la sesión (directorio, historial...).
     */
    void execute(String[] args, FileSession session);

    /**
     * Nombre(s) por los que se invoca este comando en el shell.
     * El primer elemento es el nombre principal; el resto son alias.
     */
    String[] names();

    /** Texto breve de ayuda que se muestra en el comando 'help'. */
    String helpText();
}
