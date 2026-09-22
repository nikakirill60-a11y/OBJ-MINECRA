package ru.objminecra.stalkerjackets.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import ru.objminecra.stalkerjackets.StalkerJackets;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Модель брони, построенная из OBJ-файла.
 *
 * <p>OBJ ({@code assets/stalkerjackets/models/entity/jacket.obj}) должен содержать 6 объектов
 * ({@code o ...}): {@code head}, {@code chest}, {@code armL}, {@code armR}, {@code legL}, {@code legR}.
 * Каждая часть автоматически подгоняется под пропорции ванильной модели игрока и крепится
 * к соответствующей кости, поэтому броня анимируется (ходьба, бег, присед, взмахи рук).</p>
 *
 * <p>Геометрия общая для всех курток, текстура у каждой своя
 * ({@code assets/stalkerjackets/textures/entity/*.png}).</p>
 */
public class JacketArmorModel extends HumanoidModel<LivingEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation MODEL =
            new ResourceLocation(StalkerJackets.MOD_ID, "models/entity/jacket.obj");

    /**
     * Зеркалить ли модель по Z. Проверено для jacket.obj: перед смотрит в +Z,
     * как и у ванильной модели (лицо/пуговицы спереди) — зеркалить НЕ надо.
     * Если заменишь OBJ и костюм окажется задом наперёд — поставь true.
     */
    private static final boolean MIRROR_Z = false;

    // Индексы частей.
    private static final int HEAD = 0;
    private static final int CHEST = 1;
    private static final int ARM_L = 2;
    private static final int ARM_R = 3;
    private static final int LEG_L = 4;
    private static final int LEG_R = 5;

    /** Сколько float на вершину: x, y, z, u, v, nx, ny, nz. */
    private static final int STRIDE = 8;

    private static volatile Geometry GEOMETRY;
    private static volatile boolean LOAD_FAILED;

    public JacketArmorModel(ModelPart root) {
        super(root);
    }

    private static Geometry geometry() {
        Geometry g = GEOMETRY;
        if (g == null && !LOAD_FAILED) {
            synchronized (JacketArmorModel.class) {
                g = GEOMETRY;
                if (g == null && !LOAD_FAILED) {
                    g = load();
                    if (g == null) {
                        LOAD_FAILED = true;
                    } else {
                        GEOMETRY = g;
                    }
                }
            }
        }
        return GEOMETRY;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        Geometry g = geometry();
        if (g == null) {
            return; // Ошибка уже залогирована; лучше невидимая броня, чем мусор на экране.
        }
        // 'hat' намеренно не рендерим: капюшон полностью закрывает голову.
        renderPart(poseStack, consumer, this.head, g.parts[HEAD], packedLight, packedOverlay, red, green, blue, alpha);
        renderPart(poseStack, consumer, this.body, g.parts[CHEST], packedLight, packedOverlay, red, green, blue, alpha);
        renderPart(poseStack, consumer, this.leftArm, g.parts[ARM_L], packedLight, packedOverlay, red, green, blue, alpha);
        renderPart(poseStack, consumer, this.rightArm, g.parts[ARM_R], packedLight, packedOverlay, red, green, blue, alpha);
        renderPart(poseStack, consumer, this.leftLeg, g.parts[LEG_L], packedLight, packedOverlay, red, green, blue, alpha);
        renderPart(poseStack, consumer, this.rightLeg, g.parts[LEG_R], packedLight, packedOverlay, red, green, blue, alpha);
    }

    private static void renderPart(PoseStack poseStack, VertexConsumer consumer, ModelPart part, float[] data,
                                   int packedLight, int packedOverlay,
                                   float red, float green, float blue, float alpha) {
        if (data == null || data.length == 0) {
            return;
        }
        poseStack.pushPose();
        part.translateAndRotate(poseStack);
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        // Пивоты читаем из самой кости (уже скопированы с модели сущности) —
        // геометрия задана в абсолютных координатах модели, вычитаем пивот.
        float px = part.x;
        float py = part.y;
        float pz = part.z;
        for (int i = 0; i + STRIDE <= data.length; i += STRIDE) {
            consumer.vertex(pose,
                            (data[i] - px) * 0.0625F,
                            (data[i + 1] - py) * 0.0625F,
                            (data[i + 2] - pz) * 0.0625F)
                    .color(red, green, blue, alpha)
                    .uv(data[i + 3], data[i + 4])
                    .overlayCoords(packedOverlay)
                    .uv2(packedLight)
                    .normal(normal, data[i + 5], data[i + 6], data[i + 7])
                    .endVertex();
        }
        poseStack.popPose();
    }

    // ------------------------------------------------------------------ загрузка OBJ

    private static Geometry load() {
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(MODEL);
            if (resource.isEmpty()) {
                LOGGER.error("[stalkerjackets] Missing model file: {}", MODEL);
                return null;
            }
            List<float[]> verts = new ArrayList<>();
            List<float[]> uvs = new ArrayList<>();
            List<float[]> normals = new ArrayList<>();
            @SuppressWarnings("unchecked")
            List<int[]>[] partCorners = new List[6];
            for (int i = 0; i < 6; i++) {
                partCorners[i] = new ArrayList<>();
            }
            int current = -1;
            try (InputStream in = resource.get().open();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("o ")) {
                        current = partIndex(line.substring(1).trim());
                    } else if (line.startsWith("v ")) {
                        verts.add(parseFloats(line, 3));
                    } else if (line.startsWith("vt ")) {
                        uvs.add(parseFloats(line, 2));
                    } else if (line.startsWith("vn ")) {
                        normals.add(parseFloats(line, 3));
                    } else if (line.startsWith("f ") && current >= 0) {
                        String[] tokens = line.substring(2).trim().split("\\s+");
                        int[][] corners = new int[tokens.length][];
                        for (int i = 0; i < tokens.length; i++) {
                            corners[i] = parseFace(tokens[i], verts.size(), uvs.size(), normals.size());
                        }
                        // Триангуляция веером (наш файл уже из треугольников, это на всякий случай).
                        for (int i = 1; i + 1 < corners.length; i++) {
                            partCorners[current].add(corners[0]);
                            partCorners[current].add(corners[i]);
                            partCorners[current].add(corners[i + 1]);
                        }
                    }
                }
            }
            return build(verts, uvs, normals, partCorners);
        } catch (Exception e) {
            LOGGER.error("[stalkerjackets] Failed to load {}", MODEL, e);
            return null;
        }
    }

    private static int partIndex(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        if (n.contains("head") || n.contains("helmet") || n.contains("hood")) {
            return HEAD;
        }
        if (n.contains("chest") || n.contains("body") || n.contains("torso") || n.contains("jacket")) {
            return CHEST;
        }
        if (n.contains("arm")) {
            return (n.contains("left") || n.endsWith("l")) ? ARM_L : ARM_R;
        }
        if (n.contains("leg")) {
            return (n.contains("left") || n.endsWith("l")) ? LEG_L : LEG_R;
        }
        return -1;
    }

    private static float[] parseFloats(String line, int count) {
        String[] tokens = line.trim().split("\\s+");
        float[] out = new float[count];
        for (int i = 0; i < count && i + 1 < tokens.length; i++) {
            out[i] = Float.parseFloat(tokens[i + 1]);
        }
        return out;
    }

    /** Разбирает вершину грани (v/vt/vn, v//vn, v/vt, v), поддерживает относительные индексы. */
    private static int[] parseFace(String token, int vertCount, int uvCount, int normalCount) {
        String[] parts = token.split("/", -1);
        int vi = resolveIndex(Integer.parseInt(parts[0]), vertCount);
        int vti = -1;
        int vni = -1;
        if (parts.length > 1 && !parts[1].isEmpty()) {
            vti = resolveIndex(Integer.parseInt(parts[1]), uvCount);
        }
        if (parts.length > 2 && !parts[2].isEmpty()) {
            vni = resolveIndex(Integer.parseInt(parts[2]), normalCount);
        }
        return new int[]{vi, vti, vni};
    }

    private static int resolveIndex(int raw, int count) {
        int idx = raw < 0 ? count + raw : raw - 1;
        return (idx >= 0 && idx < count) ? idx : -1;
    }

    // ------------------------------------------------------- подгонка под модель игрока

    private static Geometry build(List<float[]> verts, List<float[]> uvs, List<float[]> normals,
                                  List<int[]>[] partCorners) {
        // Рамки каждой части.
        float[][] min = new float[6][3];
        float[][] max = new float[6][3];
        for (int p = 0; p < 6; p++) {
            for (int a = 0; a < 3; a++) {
                min[p][a] = Float.POSITIVE_INFINITY;
                max[p][a] = Float.NEGATIVE_INFINITY;
            }
            for (int[] corner : partCorners[p]) {
                if (corner[0] < 0) {
                    continue;
                }
                float[] v = verts.get(corner[0]);
                for (int a = 0; a < 3; a++) {
                    if (v[a] < min[p][a]) {
                        min[p][a] = v[a];
                    }
                    if (v[a] > max[p][a]) {
                        max[p][a] = v[a];
                    }
                }
            }
        }
        if (partCorners[HEAD].isEmpty() || partCorners[LEG_L].isEmpty() || partCorners[LEG_R].isEmpty()) {
            LOGGER.error("[stalkerjackets] Model {} must contain head/legL/legR objects", MODEL);
            return null;
        }

        // Единый масштаб: ступни -> y=24, макушка -> y=-8 (всего 32 пикселя модели).
        float feet = Math.min(min[LEG_L][1], min[LEG_R][1]);
        float headTop = max[HEAD][1];
        float height = headTop - feet;
        if (!(height > 0.0001F)) {
            LOGGER.error("[stalkerjackets] Bad model height in {}", MODEL);
            return null;
        }
        float scale = 32.0F / height;

        // Целевые центры частей (координаты модели игрока, 1 ед. = 1 пиксель).
        float[] centerX = {0.0F, 0.0F, 6.0F, -6.0F, 1.95F, -1.95F};
        float[] centerZ = {0.0F, 0.0F, 0.0F, 0.0F, 0.78F, 0.78F};
        float[] spanY = {8.0F, 12.0F, 12.0F, 12.0F, 12.0F, 12.0F};

        // Отражения: Y переворачивается всегда (в OBJ +Y вверх, в модели игрока +Y вниз),
        // Z — только при MIRROR_Z. Нечётное число отражений требует смены обхода треугольника.
        int flips = 1 + (MIRROR_Z ? 1 : 0);
        boolean swapWinding = (flips % 2) == 1;

        Geometry geometry = new Geometry();
        for (int p = 0; p < 6; p++) {
            List<int[]> corners = partCorners[p];
            if (corners.isEmpty()) {
                continue;
            }
            float cx = (min[p][0] + max[p][0]) * 0.5F;
            float cz = (min[p][2] + max[p][2]) * 0.5F;
            // Сустав: голова крепится шеей (низ) к y=0; грудь/руки верхом к y=0; ноги верхом к y=12.
            float joint;
            float target;
            if (p == HEAD) {
                joint = min[p][1];
                target = 0.0F;
            } else if (p == LEG_L || p == LEG_R) {
                joint = max[p][1];
                target = 12.0F;
            } else {
                joint = max[p][1];
                target = 0.0F;
            }
            // Лёгкая подгонка длины части (только если отличие в пределах ~10%).
            float yScale = 1.0F;
            float objSpan = max[p][1] - min[p][1];
            if (objSpan > 0.0001F) {
                float ratio = spanY[p] / (objSpan * scale);
                if (ratio >= 0.92F && ratio <= 1.10F) {
                    yScale = ratio;
                }
            }
            // Чуть раздуваем части в ширину/глубину, чтобы закрыть швы (как утолщение брони).
            float inflate = (p == HEAD) ? 0.4F : 0.3F;

            float[] data = new float[corners.size() * STRIDE];
            float[] triPos = new float[9];
            int[] order = swapWinding ? new int[]{0, 2, 1} : new int[]{0, 1, 2};
            for (int t = 0; t + 2 < corners.size(); t += 3) {
                // Сначала трансформируем позиции треугольника...
                for (int k = 0; k < 3; k++) {
                    int[] corner = corners.get(t + order[k]);
                    float x = 0.0F;
                    float y = 0.0F;
                    float z = 0.0F;
                    if (corner[0] >= 0) {
                        float[] v = verts.get(corner[0]);
                        x = v[0];
                        y = v[1];
                        z = v[2];
                    }
                    if (MIRROR_Z) {
                        z = -z;
                    }
                    triPos[k * 3] = centerX[p] + (x - cx) * scale + Math.signum(x - cx) * inflate;
                    triPos[k * 3 + 1] = target - (y - joint) * scale * yScale;
                    triPos[k * 3 + 2] = centerZ[p] + (z - cz) * scale + Math.signum(z - cz) * inflate;
                }
                // ...затем выписываем вершины (нормали из файла, иначе геометрическая).
                float[] faceNormal = null;
                boolean needFaceNormal = false;
                for (int k = 0; k < 3; k++) {
                    if (corners.get(t + order[k])[2] < 0) {
                        needFaceNormal = true;
                        break;
                    }
                }
                if (needFaceNormal) {
                    faceNormal = faceNormal(triPos);
                }
                for (int k = 0; k < 3; k++) {
                    int[] corner = corners.get(t + order[k]);
                    int o = (t + k) * STRIDE;
                    data[o] = triPos[k * 3];
                    data[o + 1] = triPos[k * 3 + 1];
                    data[o + 2] = triPos[k * 3 + 2];
                    if (corner[1] >= 0) {
                        float[] uv = uvs.get(corner[1]);
                        data[o + 3] = uv[0];
                        data[o + 4] = 1.0F - uv[1]; // в OBJ v=0 снизу, в Minecraft сверху
                    } else {
                        data[o + 3] = 0.0F;
                        data[o + 4] = 0.0F;
                    }
                    float nx;
                    float ny;
                    float nz;
                    if (corner[2] >= 0) {
                        float[] n = normals.get(corner[2]);
                        nx = n[0];
                        ny = -n[1];
                        nz = MIRROR_Z ? -n[2] : n[2];
                    } else {
                        nx = faceNormal[0];
                        ny = faceNormal[1];
                        nz = faceNormal[2];
                    }
                    float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                    if (len < 0.0001F) {
                        nx = 0.0F;
                        ny = 1.0F;
                        nz = 0.0F;
                        len = 1.0F;
                    }
                    data[o + 5] = nx / len;
                    data[o + 6] = ny / len;
                    data[o + 7] = nz / len;
                }
            }
            geometry.parts[p] = data;
        }
        int tris = 0;
        for (int p = 0; p < 6; p++) {
            tris += partCorners[p].size() / 3;
        }
        LOGGER.info("[stalkerjackets] Loaded {} ({} tris, scale {})", MODEL, tris, scale);
        return geometry;
    }

    private static float[] faceNormal(float[] triPos) {
        float ux = triPos[3] - triPos[0];
        float uy = triPos[4] - triPos[1];
        float uz = triPos[5] - triPos[2];
        float vx = triPos[6] - triPos[0];
        float vy = triPos[7] - triPos[1];
        float vz = triPos[8] - triPos[2];
        float nx = uy * vz - uz * vy;
        float ny = uz * vx - ux * vz;
        float nz = ux * vy - uy * vx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 0.0001F) {
            return new float[]{0.0F, 1.0F, 0.0F};
        }
        return new float[]{nx / len, ny / len, nz / len};
    }

    private static final class Geometry {
        final float[][] parts = new float[6][];
    }
}
