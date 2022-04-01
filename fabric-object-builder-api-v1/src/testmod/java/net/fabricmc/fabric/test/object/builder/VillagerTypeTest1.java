/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.fabric.test.object.builder;

import net.fabricmc.api.ModInitializer;

public class VillagerTypeTest1 implements ModInitializer {
	@Override
	public void onInitialize() {
		/* TODO: 22w13oneblockatatime too lazy to fix/port.
		TradeOfferHelper.registerVillagerOffers(VillagerProfession.ARMORER, 1, factories -> {
			factories.add(new SimpleTradeFactory(new TradeOffer(new ItemStack(Items.GOLD_INGOT, 3), new ItemStack(Items.NETHERITE_SCRAP, 4), new ItemStack(Items.NETHERITE_INGOT), 2, 6, 0.15F)));
		});

		TradeOfferHelper.registerWanderingTraderOffers(1, factories -> {
			factories.add(new SimpleTradeFactory(new TradeOffer(new ItemStack(Items.GOLD_INGOT, 3), new ItemStack(Items.NETHERITE_SCRAP, 4), new ItemStack(Items.NETHERITE_INGOT), 2, 6, 0.35F)));
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			dispatcher.register(literal("fabric_refreshtrades").executes(context -> {
				TradeOfferHelper.refreshOffers();
				context.getSource().sendFeedback(new LiteralText("Refreshed trades"), false);
				return 1;
			}));

			dispatcher.register(literal("fabric_applywandering_trades")
					.then(argument("entity", entity()).executes(context -> {
						final Entity entity = getEntity(context, "entity");

						if (!(entity instanceof WanderingTraderEntity)) {
							throw new SimpleCommandExceptionType(new LiteralText("Entity is not a wandering trader")).create();
						}

						WanderingTraderEntity trader = (WanderingTraderEntity) entity;
						trader.getOffers().clear();

						for (TradeOffers.Factory[] value : TradeOffers.WANDERING_TRADER_TRADES.values()) {
							for (TradeOffers.Factory factory : value) {
								final TradeOffer result = factory.create(trader, new Random());

								if (result == null) {
									continue;
								}

								trader.getOffers().add(result);
							}
						}

						return 1;
					})));
		});
		 */
	}
}
