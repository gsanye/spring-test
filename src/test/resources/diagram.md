classDiagram
direction BT
class AbstractAdvisorAutoProxyCreator
class AbstractAutoProxyCreator
class AnnotationAwareAspectJAutoProxyCreator
class AopInfrastructureBean {
<<Interface>>

}
class AspectJAwareAdvisorAutoProxyCreator
class Aware {
<<Interface>>

}
class BeanClassLoaderAware {
<<Interface>>

}
class BeanFactoryAware {
<<Interface>>

}
class BeanPostProcessor {
<<Interface>>

}
class DisposableBean {
<<Interface>>

}
class InstantiationAwareBeanPostProcessor {
<<Interface>>

}
class Ordered {
<<Interface>>

}
class ProxyConfig
class ProxyProcessorSupport
class Serializable {
<<Interface>>

}
class SmartInitializingSingleton {
<<Interface>>

}
class SmartInstantiationAwareBeanPostProcessor {
<<Interface>>

}

AbstractAdvisorAutoProxyCreator  -->  AbstractAutoProxyCreator 
AbstractAutoProxyCreator  ..>  BeanFactoryAware 
AbstractAutoProxyCreator  -->  ProxyProcessorSupport 
AbstractAutoProxyCreator  ..>  SmartInstantiationAwareBeanPostProcessor 
AnnotationAwareAspectJAutoProxyCreator  -->  AspectJAwareAdvisorAutoProxyCreator 
AspectJAwareAdvisorAutoProxyCreator  -->  AbstractAdvisorAutoProxyCreator 
AspectJAwareAdvisorAutoProxyCreator  ..>  DisposableBean 
AspectJAwareAdvisorAutoProxyCreator  ..>  SmartInitializingSingleton 
BeanClassLoaderAware  -->  Aware 
BeanFactoryAware  -->  Aware 
InstantiationAwareBeanPostProcessor  -->  BeanPostProcessor 
ProxyConfig  ..>  Serializable 
ProxyProcessorSupport  ..>  AopInfrastructureBean 
ProxyProcessorSupport  ..>  BeanClassLoaderAware 
ProxyProcessorSupport  ..>  Ordered 
ProxyProcessorSupport  -->  ProxyConfig 
SmartInstantiationAwareBeanPostProcessor  -->  InstantiationAwareBeanPostProcessor 
