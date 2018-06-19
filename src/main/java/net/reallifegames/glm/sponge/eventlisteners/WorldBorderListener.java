/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Tyler Bucher
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.reallifegames.glm.sponge.eventlisteners;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.flowpowered.math.vector.Vector3d;
import net.reallifegames.glm.sponge.GlMap;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.world.ChangeWorldBorderEvent;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.WorldBorder;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Optional;

/**
 * Updates clients when a world border has been changed.
 *
 * @author Tyler Bucher
 */
public class WorldBorderListener extends CoreListener {

    /**
     * Constructs a new {@link CoreListener}.
     *
     * @param pluginInstance the {@link GlMap} instance.
     */
    WorldBorderListener(@Nonnull final GlMap pluginInstance) {
        super(pluginInstance);
    }

    @Listener
    @SuppressWarnings ("Duplicates")
    public void onChange(@Nonnull final ChangeWorldBorderEvent.TargetWorld event) {
        final World world = event.getTargetWorld();
        // Check if world is available to the map.
        if (pluginInstance.getConfig().getWorldList().contains(world.getName())) {
            final Optional<WorldBorder> optionalWorldBorder = event.getNewBorder();
            pluginInstance.getWorldBorderMap().put(world.getUniqueId(), event.getNewBorder());
            pluginInstance.getBaseGlmServer().getConnections().forEach(webSocket->{
                try {
                    StringWriter stringWriter = new StringWriter();
                    JsonGenerator jsonGenerator = new JsonFactory().createGenerator(stringWriter);
                    // Start json object
                    jsonGenerator.writeStartObject();
                    // echo command back
                    jsonGenerator.writeStringField("cmd", "worldBorderUpdate");
                    // Start data block
                    jsonGenerator.writeObjectFieldStart("data");
                    jsonGenerator.writeStringField("name", world.getName());
                    jsonGenerator.writeStringField("worldId", world.getUniqueId().toString());
                    // Start world border if present
                    if (optionalWorldBorder.isPresent()) {
                        final WorldBorder border = optionalWorldBorder.get();
                        final Vector3d center = border.getCenter();
                        jsonGenerator.writeObjectFieldStart("worldBorder");
                        jsonGenerator.writeObjectFieldStart("center");
                        jsonGenerator.writeNumberField("x", center.getX());
                        jsonGenerator.writeNumberField("y", center.getY());
                        jsonGenerator.writeNumberField("z", center.getZ());
                        // close center object
                        jsonGenerator.writeEndObject();
                        jsonGenerator.writeNumberField("diameter", border.getDiameter());
                        // close world border object
                        jsonGenerator.writeEndObject();
                    }
                    // close data object
                    jsonGenerator.writeEndObject();
                    // Close json object
                    jsonGenerator.writeEndObject();
                    // Flush data and send to client
                    jsonGenerator.flush();
                    webSocket.send(stringWriter.toString());
                } catch (IOException e1) {
                    pluginInstance.getLogger().error("Json error", e1);
                }
            });
        }
    }
}
