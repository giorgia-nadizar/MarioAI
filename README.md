# Mario AI Framework 

Based on the MarioAI framework, now including a wrapper for Python. 

To package and run, execute the following commands:

```bash
mvn clean package
```

```bash
java -cp "target/*:libs/*" PlayPython
```

Note that this will start `nServers` servers, each listening on a different port, starting from `startPort`.
You can pass these parameters to the run script as plain integer numbers, with `startPort` first and `nServers` second.