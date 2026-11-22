package de.robv.android.xposed;

import java.lang.reflect.Member;

public abstract class XC_MethodHook {
    public XC_MethodHook() {}
    public XC_MethodHook(int priority) {}

    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {}
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {}

    public static final class MethodHookParam {
        public Member method;
        public Object thisObject;
        public Object[] args;

        public Object getResult() { return null; }
        public void setResult(Object result) {}
        public Throwable getThrowable() { return null; }
        public void setThrowable(Throwable throwable) {}
        public boolean hasThrowable() { return false; }
        public Object getResultOrThrowable() throws Throwable { return null; }
    }

    public static class Unhook {
        public Member getHookedMethod() { return null; }
        public XC_MethodHook getCallback() { return null; }
        public void unhook() {}
    }
}
