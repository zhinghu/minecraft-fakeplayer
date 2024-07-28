package io.github.hello09x.fakeplayer.core.manager.invsee;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.hello09x.devtools.core.transaction.PluginTranslator;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Singleton
public class DefaultInvseeImpl extends AbstractInvsee {

    @Inject
    public DefaultInvseeImpl(PluginTranslator translator) {
        super(translator);
    }

    @Override
    public void openInventory(@NotNull Player visitor, @NotNull Player visited) {
        var view = visitor.openInventory(visited.getInventory());
        super.setTitle(view, visited);
    }

}
