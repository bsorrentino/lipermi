package net.sf.lipermi.exception;

import static java.lang.String.format;

/**
 * General LipeRMI exception
 *
 * @author lipe
 * @date   05/10/2006
 */
public class LipeRMIException extends Exception {

    private static final long serialVersionUID = 7324141364282347199L;

    public LipeRMIException() {
        super();
    }

    public LipeRMIException(String message, Throwable cause) {
        super(message, cause);
    }

    public LipeRMIException(String message, Object ...args) {
        super( format( message, args) );
    }

    public LipeRMIException(Throwable cause) {
        super(cause);
    }
}
