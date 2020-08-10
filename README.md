# tinysocket

### Add annotation on Springboot main class
```
@ComponentScan(basePackages = ["com.socket.serve"])
@EnableTinyServer //on server side
@EnableTinyClient //on client side
```

### application.yml
```
//on server side
tiny:
  server:
    port: 9000
    path: /xxx
    

//on client side
tiny:
  client:
    url: 127.0.0.1
    port: 9000
    path: gate
    id: 1
    group: 2
    name: client-name
```

### create a server socket controller
```
@Component
@SocketController("test")
class TestController: TinySocket() {

    // Payload should implement Serializable
    @EventMapping("test")
    fun test(@Payload payload: Payload): String {
        //return to the controller method annotationed by @EventMapping("test") of client side in @SocketController("test")
        return "something"
    }

    @EventMapping("add")
    fun add(a: Int, b: Int): Int {
        return a+b
    }
}
```

### create a client socket sender
```
@SocketSender("test")
interface TestSenderService {

    @EventMapping("test")
    fun test(@Payload payload: Payload): String

    //By adding @Sync annotation, this request will be synchronizely
    @Sync
    @EventMapping("add")
    fun add(a: Int, b: Int): Int
}
```



### use
```
@Service
class TestService {
    @Autowired
    lateinit var testSenderService: TestSenderService
    
    @RequestMapping("/test")
    fun test(): String {
        val payload = Payload()
        return testSenderService.test(payload)
    }
    
    fun add(): Int {
        return testSenderService.add(1, 2)
    }
}
```
