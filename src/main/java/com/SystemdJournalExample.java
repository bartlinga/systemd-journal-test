package com;

import com.bustleandflurry.systemd.journal.Syslog;
import com.bustleandflurry.systemd.journal.SystemdJournalLibrary;
import org.bridj.Pointer;
import org.bridj.SizeT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;


/**
 * Systemd-Journal usage examples
 */
public class SystemdJournalExample implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SystemdJournalExample.class);
    private Pointer<Pointer<SystemdJournalLibrary.sd_journal>> sdJournalPointer;

    @Override
    public void run(String... strings) throws Exception {
        sdJournalPointer = Pointer.allocatePointer(SystemdJournalLibrary.sd_journal.class);
        this.write();
        this.read();
    }

    /**
     * Example of reading the system journal.
     */
    public void read() {
        int r;

        r = SystemdJournalLibrary.sd_journal_open(sdJournalPointer, 0);
        if(r < 0) {
            logger.error("ERROR: " + r);
        }

        Pointer<SystemdJournalLibrary.sd_journal> sdJournal = sdJournalPointer.getPointer(SystemdJournalLibrary.sd_journal.class);

        SystemdJournalLibrary.sd_journal_seek_tail(sdJournal);
        SystemdJournalLibrary.sd_journal_next(sdJournal);

        while(true) {
            SystemdJournalLibrary.sd_journal_wait(sdJournal, -1);
            SystemdJournalLibrary.sd_journal_next(sdJournal);

            Pointer<Pointer<?>> dataPointer = Pointer.allocatePointer();
            Pointer<SizeT> sizePointer = Pointer.allocateSizeT();

            while(SystemdJournalLibrary.sd_journal_enumerate_data(sdJournal, dataPointer, sizePointer) > 0) {
                Pointer<Byte> data = dataPointer.as(Byte.class);
                logger.info(data.getPointer(Byte.class).getCString());
            }

            SystemdJournalLibrary.sd_journal_restart_data(sdJournal);
        }
    }

    /**
     *
     */
    public void write()  {
        int r;

        /*
        sd_journal_print() may be used to submit simple, plain text log entries to the system journal. The first
        argument is a priority value. This is followed by a format string and its parameters, similar to printf(3) or
        syslog(3). The priority value is one of LOG_EMERG, LOG_ALERT, LOG_CRIT, LOG_ERR, LOG_WARNING, LOG_NOTICE,
        LOG_INFO, LOG_DEBUG, as defined in syslog.h, see syslog(3) for details. It is recommended to use this call to
        submit log messages in the application locale or system locale and in UTF-8 format, but no such restrictions
        are enforced.
        */

        Pointer<Byte> message1 = Pointer.pointerToCString("Test: %s");
        Pointer<Byte> target1 = Pointer.pointerToCString("example1");
        r = SystemdJournalLibrary.sd_journal_print(Syslog.LOG_INFO, message1, target1);
        if(r < 0) {
            logger.error("Linux error code: {} ", r);
        }

        // Note: It makes more sense to stick with String.format()
        Pointer<Byte> message2 = Pointer.pointerToCString(String.format("Test: %s", "example2"));
        SystemdJournalLibrary.sd_journal_print(Syslog.LOG_INFO, message2);
        if(r < 0) {
            logger.error("Linux error code: {} ", r);
        }

        /*
        sd_journal_printv() is similar to sd_journal_print() but takes a variable argument list encapsulated in an
        object of type va_list (see stdarg(3) for more information) instead of the format string. It is otherwise
        equivalent in behavior.
        */

        // TODO: for the sake of completeness

        /*
        sd_journal_send() may be used to submit structured log entries to the system journal. It takes a series of
        format strings, each immediately followed by their associated parameters, terminated by NULL. The strings
        passed should be of the format "VARIABLE=value". The variable name must be in uppercase and consist only of
        characters, numbers and underscores, and may not begin with an underscore. (All assignments that do not follow
        this syntax will be ignored.) The value can be of any size and format. It is highly recommended to submit text
        strings formatted in the UTF-8 character encoding only, and submit binary fields only when formatting in UTF-8
        strings is not sensible. A number of well known fields are defined, see systemd.journal-fields(7) for details,
        but additional application defined fields may be used. A variable may be assigned more than one value per entry.
        */

        Pointer<Byte> message3 = Pointer.pointerToCString("MESSAGE=%s");
        Pointer<Byte> target2 = Pointer.pointerToCString("example3");
        Pointer<Byte> message4 = Pointer.pointerToCString("CUSTOM_FIELD=%s");
        Pointer<Byte> target3 = Pointer.pointerToCString("example4");
        SystemdJournalLibrary.sd_journal_send(message3, target2, message4, target3, null);
        if(r < 0) {
            logger.error("Linux error code: {} ", r);
        }

        /*
        sd_journal_sendv() is similar to sd_journal_send() but takes an array of struct iovec (as defined in uio.h, see
        readv(3) for details) instead of the format string. Each structure should reference one field of the entry to
        submit. The second argument specifies the number of structures in the array.  sd_journal_sendv() is
        particularly useful to submit binary objects to the journal where that is necessary.
        */

        //TODO: The SystemdJournalLibrary needs support for the struct iovec

        /*
        sd_journal_perror() is a similar to perror(3) and writes a message to the journal that consists of the passed
        string, suffixed with ": " and a human readable representation of the current error code stored in errno(3). If
        the message string is passed as NULL or empty string, only the error string representation will be written,
        prefixed with nothing. An additional journal field ERRNO= is included in the entry containing the numeric error
        code formatted as decimal string. The log priority used is LOG_ERR (3).

        Note that sd_journal_send() is a wrapper around sd_journal_sendv() to make it easier to use when only text
        strings shall be submitted. Also, the following two calls are mostly equivalent:

           sd_journal_print(LOG_INFO, "Hello World, this is PID %lu!", (unsigned long) getpid());

           sd_journal_send("MESSAGE=Hello World, this is PID %lu!", (unsigned long) getpid(),
                           "PRIORITY=%i", LOG_INFO,
                           NULL);

        Note that these calls implicitly add fields for the source file, function name and code line where invoked.
        This is implemented with macros. If this is not desired, it can be turned off by defining
        SD_JOURNAL_SUPPRESS_LOCATION before including sd-journal.h.
        */

        /*
        syslog(3) and sd_journal_print() may largely be used interchangeably functionality-wise. However, note that log
        messages logged via the former take a different path to the journal server than the later, and hence global
        chronological ordering between the two streams cannot be guaranteed. Using sd_journal_print() has the benefit
        of logging source code line, filenames, and functions as meta data along all entries, and guaranteeing
        chronological ordering with structured log entries that are generated via sd_journal_send(). Using syslog() has
        the benefit of being more portable.
        */
    }
}
