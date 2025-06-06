package io.xpipe.app.ext;

import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.Session;
import io.xpipe.core.store.SessionListener;
import io.xpipe.core.util.FailableSupplier;

import lombok.Getter;

@Getter
public class ShellSession extends Session {

    private final FailableSupplier<ShellControl> supplier;
    private final ShellControl shellControl;

    public ShellSession(SessionListener listener, FailableSupplier<ShellControl> supplier) throws Exception {
        super(listener);
        this.supplier = supplier;
        this.shellControl = createControl();
    }

    public void start() throws Exception {
        if (shellControl.isRunning(true)) {
            return;
        } else {
            stop();
        }

        try {
            shellControl.start();
        } catch (Exception ex) {
            try {
                stop();
            } catch (Exception stopEx) {
                ex.addSuppressed(stopEx);
            }
            throw ex;
        }
    }

    private ShellControl createControl() throws Exception {
        var pc = supplier.get();
        pc.onStartupFail(shellControl -> {
            listener.onStateChange(false);
        });
        pc.onInit(shellControl -> {
            listener.onStateChange(true);
        });
        pc.onKill(() -> {
            listener.onStateChange(false);
        });
        // Listen for parent exit as onExit is called before exit is completed
        // In case it is stuck, we would not get the right status otherwise
        pc.getParentControl().ifPresent(p -> {
            p.onExit(shellControl -> {
                listener.onStateChange(false);
            });
        });
        return pc;
    }

    public boolean isRunning() {
        return shellControl.isRunning(true);
    }

    public void stop() throws Exception {
        shellControl.shutdown();
    }
}
