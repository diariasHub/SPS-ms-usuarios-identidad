package cl.rednorte.ms_usuarios.event;

public interface DomainEventPublisher {
    void publish(Object event);
}
