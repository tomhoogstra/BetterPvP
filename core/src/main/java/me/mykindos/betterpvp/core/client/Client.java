package me.mykindos.betterpvp.core.client;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Optional;

@Setter
@Getter
@Builder
public class Client {

    String uuid;
    String name;
    @Builder.Default
    Rank rank = Rank.PLAYER;

    boolean administrating;

    private final HashMap<Enum<?>, Object> properties = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getProperty(Enum<?> key) {
        return Optional.ofNullable((T) properties.getOrDefault(key, null));
    }

    public <T> Optional<T> getProperty(Enum<?> key, Class<T> type) {
        return Optional.ofNullable(type.cast(properties.getOrDefault(key, null)));
    }

    public void putProperty(Enum<?> key, Object object) {
        properties.put(key, object);
    }

    public boolean hasRank(Rank rank) {
        return this.rank.getId() >= rank.getId();
    }

}
